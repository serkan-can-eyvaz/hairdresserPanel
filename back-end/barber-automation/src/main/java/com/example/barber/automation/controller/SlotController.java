package com.example.barber.automation.controller;

import com.example.barber.automation.dto.SlotResponse;
import com.example.barber.automation.service.SlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Slot (Müsait Saatler) REST Controller
 */
@RestController
@RequestMapping("/tenants/{tenantId}/slots")
@Tag(name = "Slot Management", description = "Müsait saat yönetimi API'leri")
public class SlotController {
    
    private final SlotService slotService;
    
    @Autowired
    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }
    
    /**
     * Belirli tarih ve hizmet için müsait slot'ları getir
     */
    @GetMapping("/daily")
    @Operation(summary = "Müsait slot'lar", description = "Belirtilen tarih ve hizmet için müsait saatleri getirir")
    public ResponseEntity<SlotResponse> getAvailableSlots(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId,
            @Parameter(description = "Hizmet ID'si") @RequestParam Long serviceId,
            @Parameter(description = "Tarih (yyyy-MM-dd)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        SlotResponse slots = slotService.getAvailableSlots(tenantId, serviceId, date);
        return ResponseEntity.ok(slots);
    }
    
    /**
     * Gelecek 7 gün için müsait slot'ları getir
     */
    @GetMapping("/weekly")
    @Operation(summary = "Haftalık müsait slot'lar", description = "Gelecek 7 gün için müsait saatleri getirir")
    public ResponseEntity<List<SlotResponse>> getAvailableSlotsForWeek(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId,
            @Parameter(description = "Hizmet ID'si") @RequestParam Long serviceId) {
        List<SlotResponse> weeklySlots = slotService.getAvailableSlotsForWeek(tenantId, serviceId);
        return ResponseEntity.ok(weeklySlots);
    }
    
    /**
     * Slot müsaitlik kontrolü
     */
    @PostMapping("/check")
    @Operation(summary = "Slot müsaitlik kontrolü", description = "Belirtilen zaman diliminin müsait olup olmadığını kontrol eder")
    public ResponseEntity<SlotCheckResponse> checkSlotAvailability(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId,
            @Parameter(description = "Slot kontrolü bilgileri") @RequestBody SlotCheckRequest request) {
        boolean isAvailable = slotService.isSlotAvailable(tenantId, request.getServiceId(), request.getStartTime());
        SlotCheckResponse response = new SlotCheckResponse();
        response.setAvailable(isAvailable);
        response.setStartTime(request.getStartTime());
        return ResponseEntity.ok(response);
    }
    
    /**
     * WhatsApp bot için müsait saatleri metin formatında getir
     */
    @GetMapping("/whatsapp-format")
    @Operation(summary = "WhatsApp için müsait slot'lar", 
               description = "WhatsApp bot için formatlanmış müsait saatleri getirir")
    public ResponseEntity<String> getAvailableSlotsForWhatsApp(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long tenantId,
            @Parameter(description = "Hizmet ID'si") @RequestParam Long serviceId,
            @Parameter(description = "Tarih (yyyy-MM-dd)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String formattedSlots = slotService.getAvailableSlotsForWhatsApp(tenantId, serviceId, date);
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.TEXT_PLAIN).body(formattedSlots);
    }

    /**
     * Slot kontrol request DTO
     */
    public static class SlotCheckRequest {
        private Long serviceId;
        private LocalDateTime startTime;

        public Long getServiceId() { return serviceId; }
        public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    }

    /**
     * Slot kontrol response DTO
     */
    public static class SlotCheckResponse {
        private Boolean available;
        private LocalDateTime startTime;

        public Boolean getAvailable() { return available; }
        public void setAvailable(Boolean available) { this.available = available; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    }
}
