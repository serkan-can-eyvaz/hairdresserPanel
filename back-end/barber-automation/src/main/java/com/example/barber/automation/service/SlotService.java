package com.example.barber.automation.service;

import com.example.barber.automation.dto.SlotResponse;
import com.example.barber.automation.entity.Appointment;
import com.example.barber.automation.entity.Service;
import com.example.barber.automation.entity.TenantSetting;
import com.example.barber.automation.repository.AppointmentRepository;
import com.example.barber.automation.repository.ServiceRepository;
import com.example.barber.automation.repository.TenantSettingRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Müsait saat slot'ları hesaplama servisi
 */
@Component
public class SlotService {
    
    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;
    private final TenantSettingRepository tenantSettingRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public SlotService(AppointmentRepository appointmentRepository, 
                      ServiceRepository serviceRepository,
                      TenantSettingRepository tenantSettingRepository,
                      ObjectMapper objectMapper) {
        this.appointmentRepository = appointmentRepository;
        this.serviceRepository = serviceRepository;
        this.tenantSettingRepository = tenantSettingRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Belirli bir tarih ve hizmet için müsait slot'ları getirme
     */
    public SlotResponse getAvailableSlots(Long tenantId, Long serviceId, LocalDate date) {
        // Hizmet kontrolü
        Service service = serviceRepository.findByIdAndTenantId(serviceId, tenantId)
                .filter(s -> s.getActive())
                .orElseThrow(() -> new IllegalArgumentException("Hizmet bulunamadı: " + serviceId));
        
        // Çalışma saatleri
        Map<String, Object> workingHours = getWorkingHours(tenantId);
        List<TimeRange> workingRanges = getWorkingRangesForDate(workingHours, date);
        
        if (workingRanges.isEmpty()) {
            return new SlotResponse(date.atStartOfDay(), Collections.emptyList());
        }
        
        // Mola saatleri
        List<TimeRange> breakRanges = getBreakRanges(tenantId, date);
        
        // Randevu aralığı (varsayılan 30 dakika)
        int intervalMinutes = getBookingInterval(tenantId);
        
        // Mevcut randevular
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        List<Appointment> existingAppointments = appointmentRepository
                .findByTenantIdAndDateRange(tenantId, startOfDay, endOfDay);
        
        // Slot'ları hesapla
        List<SlotResponse.TimeSlot> availableSlots = new ArrayList<>();
        
        for (TimeRange workingRange : workingRanges) {
            LocalDateTime currentTime = workingRange.start;
            
            while (currentTime.plusMinutes(service.getDurationMinutes()).isBefore(workingRange.end) ||
                   currentTime.plusMinutes(service.getDurationMinutes()).equals(workingRange.end)) {
                
                LocalDateTime slotEnd = currentTime.plusMinutes(service.getDurationMinutes());
                
                // Mola saatlerini kontrol et
                if (isTimeInBreaks(currentTime, slotEnd, breakRanges)) {
                    currentTime = currentTime.plusMinutes(intervalMinutes);
                    continue;
                }
                
                // Lambda için final variable
                final LocalDateTime finalCurrentTime = currentTime;
                final LocalDateTime finalSlotEnd = slotEnd;
                
                // Mevcut randevularla çakışma kontrolü
                boolean isAvailable = existingAppointments.stream()
                        .noneMatch(appointment -> 
                            appointment.getStartTime().isBefore(finalSlotEnd) && 
                            appointment.getEndTime().isAfter(finalCurrentTime));
                
                if (isAvailable) {
                    availableSlots.add(new SlotResponse.TimeSlot(currentTime, slotEnd, true));
                }
                
                currentTime = currentTime.plusMinutes(intervalMinutes);
            }
        }
        
        return new SlotResponse(date.atStartOfDay(), availableSlots);
    }
    
    /**
     * Gelecek 7 gün için müsait slot'ları getirme
     */
    public List<SlotResponse> getAvailableSlotsForWeek(Long tenantId, Long serviceId) {
        List<SlotResponse> weeklySlots = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            SlotResponse dailySlots = getAvailableSlots(tenantId, serviceId, date);
            weeklySlots.add(dailySlots);
        }
        
        return weeklySlots;
    }
    
    /**
     * WhatsApp bot için müsait saatleri metin formatında getirme
     */
    public String getAvailableSlotsForWhatsApp(Long tenantId, Long serviceId, LocalDate date) {
        SlotResponse slots = getAvailableSlots(tenantId, serviceId, date);
        
        if (slots.getAvailableSlots().isEmpty()) {
            return String.format("❌ *%s* tarihinde müsait saat bulunmuyor.", 
                    date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📅 *%s* - Müsait Saatler:\n\n", 
                date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));
        
        List<SlotResponse.TimeSlot> availableSlots = slots.getAvailableSlots()
                .stream()
                .filter(SlotResponse.TimeSlot::isAvailable)
                .limit(10) // En fazla 10 slot göster
                .collect(Collectors.toList());
        
        for (int i = 0; i < availableSlots.size(); i++) {
            SlotResponse.TimeSlot slot = availableSlots.get(i);
            sb.append(String.format("%d. %s - %s\n", 
                    i + 1,
                    slot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    slot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))));
        }
        
        return sb.toString();
    }
    
    /**
     * Randevu oluşturmadan önce slot'un hala müsait olup olmadığını kontrol etme
     */
    public boolean isSlotAvailable(Long tenantId, Long serviceId, LocalDateTime startTime) {
        // Hizmet kontrolü
        Service service = serviceRepository.findByIdAndTenantId(serviceId, tenantId)
                .filter(s -> s.getActive())
                .orElseThrow(() -> new IllegalArgumentException("Hizmet bulunamadı: " + serviceId));
        
        LocalDateTime endTime = startTime.plusMinutes(service.getDurationMinutes());
        
        // Çakışan randevu kontrolü
        List<Appointment> conflictingAppointments = appointmentRepository
                .findConflictingAppointments(tenantId, startTime, endTime);
        
        return conflictingAppointments.isEmpty();
    }
    
    // Private helper methods
    
    private Map<String, Object> getWorkingHours(Long tenantId) {
        Optional<TenantSetting> workingHoursSetting = tenantSettingRepository
                .findByTenantIdAndSettingKey(tenantId, TenantSetting.Keys.WORKING_HOURS);
        
        if (workingHoursSetting.isEmpty()) {
            // Varsayılan çalışma saatleri
            return getDefaultWorkingHours();
        }
        
        try {
            return objectMapper.readValue(workingHoursSetting.get().getSettingValue(), 
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return getDefaultWorkingHours();
        }
    }
    
    private Map<String, Object> getDefaultWorkingHours() {
        Map<String, Object> defaultHours = new HashMap<>();
        
        // Pazartesi-Cumartesi 09:00-18:00
        for (String day : Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")) {
            Map<String, String> dayHours = new HashMap<>();
            dayHours.put("start", "09:00");
            dayHours.put("end", "18:00");
            dayHours.put("enabled", "true");
            defaultHours.put(day, dayHours);
        }
        
        // Pazar kapalı
        Map<String, String> sundayHours = new HashMap<>();
        sundayHours.put("enabled", "false");
        defaultHours.put("SUNDAY", sundayHours);
        
        return defaultHours;
    }
    
    private List<TimeRange> getWorkingRangesForDate(Map<String, Object> workingHours, LocalDate date) {
        String dayKey = date.getDayOfWeek().name();
        Object dayHoursObj = workingHours.get(dayKey);
        
        if (dayHoursObj == null) {
            return Collections.emptyList();
        }
        
        @SuppressWarnings("unchecked")
        Map<String, String> dayHours = (Map<String, String>) dayHoursObj;
        
        if (!"true".equals(dayHours.get("enabled"))) {
            return Collections.emptyList();
        }
        
        String startStr = dayHours.get("start");
        String endStr = dayHours.get("end");
        
        if (startStr == null || endStr == null) {
            return Collections.emptyList();
        }
        
        try {
            LocalTime startTime = LocalTime.parse(startStr);
            LocalTime endTime = LocalTime.parse(endStr);
            
            LocalDateTime start = date.atTime(startTime);
            LocalDateTime end = date.atTime(endTime);
            
            return Arrays.asList(new TimeRange(start, end));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    
    private List<TimeRange> getBreakRanges(Long tenantId, LocalDate date) {
        Optional<TenantSetting> breakHoursSetting = tenantSettingRepository
                .findByTenantIdAndSettingKey(tenantId, TenantSetting.Keys.BREAK_HOURS);
        
        if (breakHoursSetting.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> breakHours = objectMapper.readValue(
                    breakHoursSetting.get().getSettingValue(), 
                    new TypeReference<List<Map<String, String>>>() {});
            
            return breakHours.stream()
                    .filter(breakHour -> "true".equals(breakHour.get("enabled")))
                    .map(breakHour -> {
                        LocalTime start = LocalTime.parse(breakHour.get("start"));
                        LocalTime end = LocalTime.parse(breakHour.get("end"));
                        return new TimeRange(date.atTime(start), date.atTime(end));
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    
    private boolean isTimeInBreaks(LocalDateTime startTime, LocalDateTime endTime, List<TimeRange> breakRanges) {
        return breakRanges.stream()
                .anyMatch(breakRange -> 
                    startTime.isBefore(breakRange.end) && endTime.isAfter(breakRange.start));
    }
    
    private int getBookingInterval(Long tenantId) {
        Optional<TenantSetting> intervalSetting = tenantSettingRepository
                .findByTenantIdAndSettingKey(tenantId, TenantSetting.Keys.BOOKING_INTERVAL_MINUTES);
        
        if (intervalSetting.isPresent()) {
            Integer interval = intervalSetting.get().getAsInteger();
            if (interval != null && interval > 0) {
                return interval;
            }
        }
        
        return 30; // Varsayılan 30 dakika
    }
    
    /**
     * Zaman aralığı için helper class
     */
    private static class TimeRange {
        final LocalDateTime start;
        final LocalDateTime end;
        
        TimeRange(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
    }
}
