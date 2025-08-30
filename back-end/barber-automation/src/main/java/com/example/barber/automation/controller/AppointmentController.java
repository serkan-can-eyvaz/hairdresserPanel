package com.example.barber.automation.controller;

import com.example.barber.automation.dto.AppointmentDto;
import com.example.barber.automation.dto.CreateAppointmentRequest;
import com.example.barber.automation.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Appointment (Randevu) REST Controller
 */
@RestController
@RequestMapping("/tenants/{tenantId}/appointments")
@Tag(name = "Appointment Management", description = "Randevu yönetimi API'leri")
public class AppointmentController {
    
    private final AppointmentService appointmentService;
    
    @Autowired
    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }
    
    /**
     * Randevu detayı getir
     */
    @GetMapping("/{id}")
    @Operation(summary = "Randevu detayı", description = "Belirtilen ID'ye sahip randevunun detaylarını getirir")
    public ResponseEntity<AppointmentDto> getAppointmentById(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId,
            @Parameter(description = "Randevu ID'si") @PathVariable Long id) {
        return appointmentService.findById(id, tenantId)
                .map(appointment -> ResponseEntity.ok(appointment))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Aktif randevuları listele
     */
    @GetMapping("/active")
    @Operation(summary = "Aktif randevuları listele", description = "Kuaföre ait onaylanmış ve beklemedeki randevuları getirir")
    public ResponseEntity<List<AppointmentDto>> getActiveAppointments(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId) {
        List<AppointmentDto> appointments = appointmentService.findActiveAppointments(tenantId);
        return ResponseEntity.ok(appointments);
    }
    
    /**
     * Tarih aralığına göre randevuları getir
     */
    @GetMapping
    @Operation(summary = "Tarih aralığına göre randevular", description = "Belirtilen tarih aralığındaki randevuları getirir")
    public ResponseEntity<List<AppointmentDto>> getAppointmentsByDateRange(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId,
            @Parameter(description = "Başlangıç tarihi (yyyy-MM-dd'T'HH:mm:ss)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Bitiş tarihi (yyyy-MM-dd'T'HH:mm:ss)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<AppointmentDto> appointments = appointmentService.findByDateRange(tenantId, startDate, endDate);
        return ResponseEntity.ok(appointments);
    }
    
    /**
     * Müşteriye ait randevuları getir
     */
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Müşteri randevuları", description = "Belirtilen müşteriye ait tüm randevuları getirir")
    public ResponseEntity<List<AppointmentDto>> getAppointmentsByCustomer(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId,
            @Parameter(description = "Müşteri ID'si") @PathVariable Long customerId) {
        List<AppointmentDto> appointments = appointmentService.findByCustomer(tenantId, customerId);
        return ResponseEntity.ok(appointments);
    }
    
    /**
     * Bugünkü randevuları getir
     */
    @GetMapping("/today")
    @Operation(summary = "Bugünkü randevular", description = "Bugün tarihli tüm randevuları getirir")
    public ResponseEntity<List<AppointmentDto>> getTodayAppointments(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId) {
        List<AppointmentDto> appointments = appointmentService.findTodayAppointments(tenantId);
        return ResponseEntity.ok(appointments);
    }
    
    /**
     * Yaklaşan randevuları getir (gelecek 7 gün)
     */
    @GetMapping("/upcoming")
    @Operation(summary = "Yaklaşan randevular", description = "Gelecek 7 gün içindeki randevuları getirir")
    public ResponseEntity<List<AppointmentDto>> getUpcomingAppointments(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId) {
        List<AppointmentDto> appointments = appointmentService.findUpcomingAppointments(tenantId);
        return ResponseEntity.ok(appointments);
    }
    
    /**
     * Yeni randevu oluştur
     */
    @PostMapping
    @Operation(summary = "Yeni randevu oluştur", description = "Yeni randevu kaydı oluşturur")
    public ResponseEntity<AppointmentDto> createAppointment(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId,
            @Parameter(description = "Randevu bilgileri") @Valid @RequestBody CreateAppointmentRequest request) {
        try {
            AppointmentDto createdAppointment = appointmentService.createAppointment(request, tenantId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAppointment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Randevu onayла
     */
    @PostMapping("/{id}/confirm")
    @Operation(summary = "Randevu onayla", description = "Beklemedeki randevuyu onaylar")
    public ResponseEntity<AppointmentDto> confirmAppointment(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId,
            @Parameter(description = "Randevu ID'si") @PathVariable Long id) {
        try {
            AppointmentDto confirmedAppointment = appointmentService.confirmAppointment(id, tenantId);
            return ResponseEntity.ok(confirmedAppointment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Randevu tamamla
     */
    @PostMapping("/{id}/complete")
    @Operation(summary = "Randevu tamamla", description = "Randevuyu tamamlanmış olarak işaretler")
    public ResponseEntity<AppointmentDto> completeAppointment(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId,
            @Parameter(description = "Randevu ID'si") @PathVariable Long id) {
        try {
            AppointmentDto completedAppointment = appointmentService.completeAppointment(id, tenantId);
            return ResponseEntity.ok(completedAppointment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Randevu iptal et
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Randevu iptal et", description = "Randevuyu iptal eder")
    public ResponseEntity<AppointmentDto> cancelAppointment(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId,
            @Parameter(description = "Randevu ID'si") @PathVariable Long id,
            @Parameter(description = "İptal nedeni") @RequestBody(required = false) java.util.Map<String, String> cancelRequest) {
        String reason = cancelRequest != null ? cancelRequest.get("reason") : null;
        try {
            AppointmentDto cancelledAppointment = appointmentService.cancelAppointment(id, tenantId, reason);
            return ResponseEntity.ok(cancelledAppointment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Randevu güncelle
     */
    @PutMapping("/{id}")
    @Operation(summary = "Randevu güncelle", description = "Mevcut randevunun bilgilerini günceller")
    public ResponseEntity<AppointmentDto> updateAppointment(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId,
            @Parameter(description = "Randevu ID'si") @PathVariable Long id,
            @Parameter(description = "Güncellenecek randevu bilgileri") @Valid @RequestBody CreateAppointmentRequest request) {
        try {
            AppointmentDto updatedAppointment = appointmentService.updateAppointment(id, request, tenantId);
            return ResponseEntity.ok(updatedAppointment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Müşterinin aktif randevusu var mı kontrol et
     */
    @GetMapping("/customer/{customerId}/has-active")
    @Operation(summary = "Aktif randevu kontrolü", description = "Müşterinin aktif randevusu olup olmadığını kontrol eder")
    public ResponseEntity<Boolean> hasActiveAppointment(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId,
            @Parameter(description = "Müşteri ID'si") @PathVariable Long customerId) {
        boolean hasActive = appointmentService.hasActiveAppointment(tenantId, customerId);
        return ResponseEntity.ok(hasActive);
    }
    
    /**
     * Randevu istatistikleri
     */
    @GetMapping("/stats")
    @Operation(summary = "Randevu istatistikleri", description = "Kuaföre ait randevu istatistiklerini getirir")
    public ResponseEntity<Map<String, Long>> getAppointmentStats(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId) {
        Map<String, Long> stats = appointmentService.getAppointmentStats(tenantId);
        return ResponseEntity.ok(stats);
    }
}
