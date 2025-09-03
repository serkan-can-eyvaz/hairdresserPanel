package com.example.barber.automation.service;

import com.example.barber.automation.entity.Appointment;
import com.example.barber.automation.entity.Service;
import com.example.barber.automation.entity.Tenant;
import com.example.barber.automation.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Akıllı randevu sistemi - çalışma saatleri, hizmet süresi ve mola süresini dikkate alır
 */
@Service
public class SmartSchedulingService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    /**
     * Belirli bir tarih için uygun randevu saatlerini hesaplar
     * @param tenant Kuaför
     * @param service Hizmet
     * @param date Tarih
     * @return Uygun saatlerin listesi
     */
    public List<LocalTime> getAvailableTimeSlots(Tenant tenant, Service service, LocalDate date) {
        List<LocalTime> availableSlots = new ArrayList<>();
        
        // Çalışma saatlerini parse et
        LocalTime workingStart = LocalTime.parse(tenant.getWorkingHoursStart());
        LocalTime workingEnd = LocalTime.parse(tenant.getWorkingHoursEnd());
        
        // Hizmet süresi ve mola süresi
        int serviceDuration = service.getDurationMinutes();
        int breakDuration = tenant.getBreakMinutes();
        int totalSlotDuration = serviceDuration + breakDuration;
        
        // O günkü mevcut randevuları al
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        List<Appointment> existingAppointments = appointmentRepository
            .findByTenantIdAndStartTimeBetweenAndStatusNot(
                tenant.getId(), 
                startOfDay, 
                endOfDay, 
                "CANCELLED"
            );
        
        // Mevcut randevuları sırala
        existingAppointments.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
        
        // Çalışma saatleri içinde uygun slotları bul
        LocalTime currentTime = workingStart;
        
        while (currentTime.plusMinutes(serviceDuration).isBefore(workingEnd) || 
               currentTime.plusMinutes(serviceDuration).equals(workingEnd)) {
            
            // Bu saatte randevu alınabilir mi kontrol et
            if (isTimeSlotAvailable(currentTime, serviceDuration, existingAppointments, tenant)) {
                availableSlots.add(currentTime);
            }
            
            // 15 dakika aralıklarla kontrol et
            currentTime = currentTime.plusMinutes(15);
        }
        
        return availableSlots;
    }
    
    /**
     * Belirli bir saat diliminin uygun olup olmadığını kontrol eder
     */
    private boolean isTimeSlotAvailable(LocalTime startTime, int serviceDuration, 
                                      List<Appointment> existingAppointments, Tenant tenant) {
        
        LocalTime endTime = startTime.plusMinutes(serviceDuration);
        int breakDuration = tenant.getBreakMinutes();
        
        for (Appointment appointment : existingAppointments) {
            LocalTime appointmentStart = appointment.getStartTime().toLocalTime();
            LocalTime appointmentEnd = appointmentStart.plusMinutes(
                appointment.getService().getDurationMinutes()
            ).plusMinutes(breakDuration); // Mola süresi ekle
            
            // Çakışma kontrolü
            if (isTimeOverlap(startTime, endTime, appointmentStart, appointmentEnd)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * İki zaman diliminin çakışıp çakışmadığını kontrol eder
     */
    private boolean isTimeOverlap(LocalTime start1, LocalTime end1, 
                                LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
    
    /**
     * Randevu oluştururken bitiş zamanını hesaplar
     * @param startTime Başlangıç zamanı
     * @param service Hizmet
     * @param tenant Kuaför
     * @return Bitiş zamanı
     */
    public LocalDateTime calculateEndTime(LocalDateTime startTime, Service service, Tenant tenant) {
        return startTime.plusMinutes(service.getDurationMinutes());
    }
    
    /**
     * Sonraki uygun randevu zamanını hesaplar (mola süresi dahil)
     * @param lastAppointmentEnd Son randevunun bitiş zamanı
     * @param tenant Kuaför
     * @return Sonraki uygun zaman
     */
    public LocalDateTime getNextAvailableTime(LocalDateTime lastAppointmentEnd, Tenant tenant) {
        return lastAppointmentEnd.plusMinutes(tenant.getBreakMinutes());
    }
    
    /**
     * Belirli bir tarih aralığı için uygun saatleri döndürür
     * @param tenant Kuaför
     * @param service Hizmet
     * @param startDate Başlangıç tarihi
     * @param endDate Bitiş tarihi
     * @return Tarih ve uygun saatlerin map'i
     */
    public List<TimeSlotInfo> getAvailableTimeSlotsForDateRange(Tenant tenant, Service service, 
                                                               LocalDate startDate, LocalDate endDate) {
        List<TimeSlotInfo> result = new ArrayList<>();
        
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            List<LocalTime> availableSlots = getAvailableTimeSlots(tenant, service, currentDate);
            if (!availableSlots.isEmpty()) {
                result.add(new TimeSlotInfo(currentDate, availableSlots));
            }
            currentDate = currentDate.plusDays(1);
        }
        
        return result;
    }
    
    /**
     * Tarih ve uygun saatleri içeren sınıf
     */
    public static class TimeSlotInfo {
        private LocalDate date;
        private List<LocalTime> availableSlots;
        
        public TimeSlotInfo(LocalDate date, List<LocalTime> availableSlots) {
            this.date = date;
            this.availableSlots = availableSlots;
        }
        
        public LocalDate getDate() {
            return date;
        }
        
        public List<LocalTime> getAvailableSlots() {
            return availableSlots;
        }
        
        public String getFormattedDate() {
            return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        
        public List<String> getFormattedTimeSlots() {
            return availableSlots.stream()
                .map(time -> time.format(DateTimeFormatter.ofPattern("HH:mm")))
                .collect(Collectors.toList());
        }
    }
}
