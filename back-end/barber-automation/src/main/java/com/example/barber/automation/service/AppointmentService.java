package com.example.barber.automation.service;

import com.example.barber.automation.dto.AppointmentDto;
import com.example.barber.automation.dto.CreateAppointmentRequest;
import com.example.barber.automation.dto.CustomerDto;
import com.example.barber.automation.dto.ServiceDto;
import com.example.barber.automation.entity.Appointment;
import com.example.barber.automation.entity.Customer;
import com.example.barber.automation.entity.Service;
import com.example.barber.automation.entity.Tenant;
import com.example.barber.automation.repository.AppointmentRepository;
import com.example.barber.automation.repository.CustomerRepository;
import com.example.barber.automation.repository.ServiceRepository;
import com.example.barber.automation.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Appointment (Randevu) business logic service
 */
@org.springframework.stereotype.Service
@Transactional
public class AppointmentService {
    
    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;
    private final TenantRepository tenantRepository;
    private final SlotService slotService;
    private final CustomerService customerService;
    
    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
                             CustomerRepository customerRepository,
                             ServiceRepository serviceRepository,
                             TenantRepository tenantRepository,
                             SlotService slotService,
                             CustomerService customerService) {
        this.appointmentRepository = appointmentRepository;
        this.customerRepository = customerRepository;
        this.serviceRepository = serviceRepository;
        this.tenantRepository = tenantRepository;
        this.slotService = slotService;
        this.customerService = customerService;
    }
    
    /**
     * Randevu ID'sine göre bulma
     */
    public Optional<AppointmentDto> findById(Long id, Long tenantId) {
        return appointmentRepository.findByIdAndTenantId(id, tenantId)
                .map(this::convertToDto);
    }
    
    /**
     * Tenant'a ait aktif randevuları listeleme
     */
    public List<AppointmentDto> findActiveAppointments(Long tenantId) {
        return appointmentRepository.findActiveAppointmentsByTenantId(tenantId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Tenant'a ait tüm randevuları listeleme (Controller için)
     */
    public List<AppointmentDto> findAllByTenantId(Long tenantId) {
        return appointmentRepository.findByTenantIdOrderByStartTimeDesc(tenantId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Tüm randevuları listeleme
     */
    public List<AppointmentDto> findAll() {
        return appointmentRepository.findAllByOrderByStartTimeDesc()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Belirli tarih aralığındaki randevuları getirme
     */
    public List<AppointmentDto> findByDateRange(Long tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        return appointmentRepository.findByTenantIdAndDateRange(tenantId, startDate, endDate)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Müşteriye ait randevuları getirme
     */
    public List<AppointmentDto> findByCustomer(Long tenantId, Long customerId) {
        return appointmentRepository.findByTenantIdAndCustomerId(tenantId, customerId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Bugünkü randevuları getirme
     */
    public List<AppointmentDto> findTodayAppointments(Long tenantId) {
        return appointmentRepository.findTodayAppointments(tenantId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Yaklaşan randevuları getirme (gelecek 7 gün)
     */
    public List<AppointmentDto> findUpcomingAppointments(Long tenantId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekFromNow = now.plusDays(7);
        
        return appointmentRepository.findUpcomingAppointments(tenantId, now, weekFromNow)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Yeni randevu oluşturma
     */
    public AppointmentDto createAppointment(CreateAppointmentRequest request, Long tenantId) {
        // Tenant kontrolü
        Tenant tenant = tenantRepository.findById(tenantId)
                .filter(t -> t.getActive())
                .orElseThrow(() -> new IllegalArgumentException("Kuaför bulunamadı: " + tenantId));
        
        // Müşteri kontrolü/oluşturma
        Customer customer;
        if (request.getCustomerId() != null) {
            customer = customerRepository.findByIdAndTenantId(request.getCustomerId(), tenantId)
                    .filter(c -> c.getActive())
                    .orElseThrow(() -> new IllegalArgumentException("Müşteri bulunamadı: " + request.getCustomerId()));
        } else if (request.getCustomerPhone() != null) {
            // WhatsApp'tan gelen randevu - müşteri otomatik oluştur
            CustomerDto customerDto = customerService.createCustomerFromWhatsApp(
                    request.getCustomerName(), 
                    request.getCustomerPhone(), 
                    tenantId);
            customer = customerRepository.findById(customerDto.getId()).orElseThrow();
        } else {
            throw new IllegalArgumentException("Müşteri bilgisi eksik");
        }
        
        // Hizmet kontrolü
        Service service = serviceRepository.findByIdAndTenantId(request.getServiceId(), tenantId)
                .filter(s -> s.getActive())
                .orElseThrow(() -> new IllegalArgumentException("Hizmet bulunamadı: " + request.getServiceId()));
        
        // Slot müsaitlik kontrolü
        if (!slotService.isSlotAvailable(tenantId, service.getId(), request.getStartTime())) {
            throw new IllegalArgumentException("Seçilen saat artık müsait değil");
        }
        
        // Randevu oluştur
        LocalDateTime endTime = request.getStartTime().plusMinutes(service.getDurationMinutes());
        
        Appointment appointment = new Appointment(
                request.getStartTime(),
                endTime,
                tenant,
                customer,
                service
        );
        
        appointment.setNotes(request.getNotes());
        appointment.setStatus(Appointment.AppointmentStatus.PENDING);
        // Hizmet fiyatı ve para birimini randevuya işle
        try {
            if (service.getPrice() != null) {
                appointment.setTotalPrice(service.getPrice());
            }
            if (service.getCurrency() != null && !service.getCurrency().isBlank()) {
                appointment.setCurrency(service.getCurrency());
            } else {
                // Varsayılan para birimi
                appointment.setCurrency("TRY");
            }
        } catch (Exception ignored) {
            // Herhangi bir serileştirme/nullable probleminde randevuyu bloke etme
            if (appointment.getCurrency() == null) {
                appointment.setCurrency("TRY");
            }
        }
        
        Appointment savedAppointment = appointmentRepository.save(appointment);
        return convertToDto(savedAppointment);
    }
    
    /**
     * Randevu onaylama
     */
    public AppointmentDto confirmAppointment(Long id, Long tenantId) {
        Appointment appointment = appointmentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Randevu bulunamadı: " + id));
        
        if (appointment.getStatus() != Appointment.AppointmentStatus.PENDING) {
            throw new IllegalArgumentException("Sadece beklemedeki randevular onaylanabilir");
        }
        
        appointment.setStatus(Appointment.AppointmentStatus.CONFIRMED);
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        
        return convertToDto(updatedAppointment);
    }
    
    /**
     * Randevu tamamlama
     */
    public AppointmentDto completeAppointment(Long id, Long tenantId) {
        Appointment appointment = appointmentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Randevu bulunamadı: " + id));
        
        if (appointment.isCancelled()) {
            throw new IllegalArgumentException("İptal edilmiş randevu tamamlanamaz");
        }
        
        appointment.setStatus(Appointment.AppointmentStatus.COMPLETED);
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        
        return convertToDto(updatedAppointment);
    }
    
    /**
     * Randevu iptal etme
     */
    public AppointmentDto cancelAppointment(Long id, Long tenantId, String reason) {
        Appointment appointment = appointmentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Randevu bulunamadı: " + id));
        
        if (appointment.isCompleted()) {
            throw new IllegalArgumentException("Tamamlanmış randevu iptal edilemez");
        }
        
        appointment.setStatus(Appointment.AppointmentStatus.CANCELLED);
        if (reason != null) {
            appointment.setNotes(appointment.getNotes() != null ? 
                    appointment.getNotes() + "\n\nİptal nedeni: " + reason : 
                    "İptal nedeni: " + reason);
        }
        
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return convertToDto(updatedAppointment);
    }
    
    /**
     * Randevu güncelleme
     */
    public AppointmentDto updateAppointment(Long id, CreateAppointmentRequest request, Long tenantId) {
        Appointment appointment = appointmentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Randevu bulunamadı: " + id));
        
        if (appointment.isCompleted() || appointment.isCancelled()) {
            throw new IllegalArgumentException("Tamamlanmış veya iptal edilmiş randevu güncellenemez");
        }
        
        // Yeni zaman için slot kontrolü (mevcut randevu hariç)
        if (!request.getStartTime().equals(appointment.getStartTime())) {
            Service service = appointment.getService();
            LocalDateTime newEndTime = request.getStartTime().plusMinutes(service.getDurationMinutes());
            
            List<Appointment> conflictingAppointments = appointmentRepository
                    .findConflictingAppointments(tenantId, request.getStartTime(), newEndTime)
                    .stream()
                    .filter(a -> !a.getId().equals(id))
                    .collect(Collectors.toList());
            
            if (!conflictingAppointments.isEmpty()) {
                throw new IllegalArgumentException("Seçilen saat başka bir randevuyla çakışıyor");
            }
            
            appointment.setStartTime(request.getStartTime());
            appointment.setEndTime(newEndTime);
        }
        
        appointment.setNotes(request.getNotes());
        
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return convertToDto(updatedAppointment);
    }
    
    /**
     * Müşterinin aktif randevusu olup olmadığını kontrol etme
     */
    public boolean hasActiveAppointment(Long tenantId, Long customerId) {
        return appointmentRepository.hasActiveAppointment(tenantId, customerId);
    }
    
    /**
     * Randevu istatistikleri
     */
    public Map<String, Long> getAppointmentStats(Long tenantId) {
        Map<String, Long> stats = new HashMap<>();
        
        stats.put("pending", appointmentRepository.countByTenantIdAndStatus(tenantId, Appointment.AppointmentStatus.PENDING));
        stats.put("confirmed", appointmentRepository.countByTenantIdAndStatus(tenantId, Appointment.AppointmentStatus.CONFIRMED));
        stats.put("completed", appointmentRepository.countByTenantIdAndStatus(tenantId, Appointment.AppointmentStatus.COMPLETED));
        stats.put("cancelled", appointmentRepository.countByTenantIdAndStatus(tenantId, Appointment.AppointmentStatus.CANCELLED));
        
        return stats;
    }
    
    /**
     * WhatsApp bot için randevu onay mesajı
     */
    public String getAppointmentConfirmationMessage(AppointmentDto appointment) {
        return String.format(
                "✅ *Randevunuz Onaylandı!*\n\n" +
                "👤 *Müşteri:* %s\n" +
                "🔸 *Hizmet:* %s\n" +
                "📅 *Tarih:* %s\n" +
                "⏰ *Saat:* %s - %s\n" +
                "💰 *Fiyat:* %s\n\n" +
                "📍 Randevu zamanında bekleriz! 😊",
                appointment.getCustomer().getName(),
                appointment.getService().getName(),
                appointment.getStartTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                appointment.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                appointment.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                appointment.getFormattedPrice()
        );
    }
    
    // Utility methods
    private AppointmentDto convertToDto(Appointment appointment) {
        AppointmentDto dto = new AppointmentDto();
        dto.setId(appointment.getId());
        dto.setStartTime(appointment.getStartTime());
        dto.setEndTime(appointment.getEndTime());
        dto.setStatus(appointment.getStatus());
        dto.setNotes(appointment.getNotes());
        dto.setTotalPrice(appointment.getTotalPrice());
        dto.setCurrency(appointment.getCurrency());
        dto.setReminderSent(appointment.getReminderSent());
        dto.setReminderSentAt(appointment.getReminderSentAt());
        dto.setCreatedAt(appointment.getCreatedAt());
        dto.setUpdatedAt(appointment.getUpdatedAt());
        
        // IDs
        dto.setTenantId(appointment.getTenant().getId());
        dto.setCustomerId(appointment.getCustomer().getId());
        dto.setServiceId(appointment.getService().getId());
        
        // Related entities
        dto.setCustomer(convertCustomerToDto(appointment.getCustomer()));
        dto.setService(convertServiceToDto(appointment.getService()));
        
        return dto;
    }
    
    private CustomerDto convertCustomerToDto(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setPhoneNumber(customer.getPhoneNumber());
        dto.setEmail(customer.getEmail());
        return dto;
    }
    
    private ServiceDto convertServiceToDto(Service service) {
        ServiceDto dto = new ServiceDto();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setDescription(service.getDescription());
        dto.setDurationMinutes(service.getDurationMinutes());
        dto.setPrice(service.getPrice());
        dto.setCurrency(service.getCurrency());
        return dto;
    }
}
