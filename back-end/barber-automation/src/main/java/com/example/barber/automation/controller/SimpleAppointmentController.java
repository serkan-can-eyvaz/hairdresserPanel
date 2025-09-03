package com.example.barber.automation.controller;

import com.example.barber.automation.dto.AppointmentDto;
import com.example.barber.automation.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Simple Appointment (Randevu) REST Controller - Frontend için basit endpoint'ler
 */
@RestController
@RequestMapping("/appointments")
@Tag(name = "Simple Appointment Management", description = "Basit randevu yönetimi API'leri")
public class SimpleAppointmentController {

    private final AppointmentService appointmentService;

    @Autowired
    public SimpleAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Tüm randevuları listele
     */
    @GetMapping
    @Operation(summary = "Randevu listesi", description = "Tüm randevuları listeler")
    public ResponseEntity<List<AppointmentDto>> getAllAppointments(
            @Parameter(description = "Tenant ID") @RequestParam(value = "tenantId", required = false) Long tenantId) {
        
        List<AppointmentDto> appointments;
        if (tenantId != null) {
            appointments = appointmentService.findAllByTenantId(tenantId);
        } else {
            appointments = appointmentService.findAll();
        }
        
        return ResponseEntity.ok(appointments);
    }

    /**
     * ID'ye göre randevu getir
     */
    @GetMapping("/{id}")
    @Operation(summary = "Randevu detayı", description = "ID'ye göre randevu detayını getirir")
    public ResponseEntity<AppointmentDto> getAppointmentById(
            @Parameter(description = "Randevu ID") @PathVariable Long id,
            @Parameter(description = "Tenant ID") @RequestParam(value = "tenantId", required = false) Long tenantId) {
        
        if (tenantId == null) {
            tenantId = 1L; // Varsayılan tenant ID
        }
        
        return appointmentService.findById(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
