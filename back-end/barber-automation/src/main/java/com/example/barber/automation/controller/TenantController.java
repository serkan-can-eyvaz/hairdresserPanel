package com.example.barber.automation.controller;

import com.example.barber.automation.dto.TenantDto;
import com.example.barber.automation.dto.CreateTenantRequest;
import com.example.barber.automation.entity.Tenant;
import com.example.barber.automation.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tenant (Kuaför) REST Controller
 */
@RestController
@RequestMapping("/tenants")
@Tag(name = "Tenant Management", description = "Kuaför yönetimi API'leri")
public class TenantController {
    
    private final TenantService tenantService;
    
    @Autowired
    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }
    
    /**
     * Tüm aktif kuaförleri listele
     */
    @GetMapping
    @Operation(summary = "Aktif kuaförleri listele", description = "Sistemdeki tüm aktif kuaförleri getirir")
    public ResponseEntity<List<TenantDto>> getAllTenants() {
        List<TenantDto> tenants = tenantService.findAllActive();
        return ResponseEntity.ok(tenants);
    }
    
    /**
     * ID'ye göre kuaför getir
     */
    @GetMapping("/{id}")
    @Operation(summary = "Kuaför detayı", description = "Belirtilen ID'ye sahip kuaförün detaylarını getirir")
    public ResponseEntity<TenantDto> getTenantById(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long id) {
        return tenantService.findById(id)
                .map(tenant -> ResponseEntity.ok(tenant))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Telefon numarasına göre kuaför getir (WhatsApp entegrasyonu için)
     */
    @GetMapping("/by-phone/{phoneNumber}")
    @Operation(summary = "Telefon numarasına göre kuaför bul", 
               description = "WhatsApp entegrasyonu için telefon numarasına göre kuaför bulur")
    public ResponseEntity<TenantDto> getTenantByPhoneNumber(
            @Parameter(description = "Telefon numarası (+905321234567 formatında)") 
            @PathVariable String phoneNumber) {
        Tenant tenant = tenantService.findByPhoneNumber(phoneNumber);
        if (tenant != null) {
            // Tenant'ı DTO'ya çevir
            TenantDto tenantDto = new TenantDto();
            tenantDto.setId(tenant.getId());
            tenantDto.setName(tenant.getName());
            tenantDto.setPhoneNumber(tenant.getPhoneNumber());
            tenantDto.setAddress(tenant.getAddress());
            tenantDto.setEmail(tenant.getEmail());
            tenantDto.setTimezone(tenant.getTimezone());
            tenantDto.setLogoUrl(tenant.getLogoUrl());
            tenantDto.setActive(tenant.getActive());
            tenantDto.setCreatedAt(tenant.getCreatedAt());
            tenantDto.setUpdatedAt(tenant.getUpdatedAt());
            return ResponseEntity.ok(tenantDto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Kuaför adına göre arama
     */
    @GetMapping("/search")
    @Operation(summary = "Kuaför arama", description = "Kuaför adına göre arama yapar")
    public ResponseEntity<List<TenantDto>> searchTenants(
            @Parameter(description = "Aranacak kuaför adı") @RequestParam String name) {
        List<TenantDto> tenants = tenantService.searchByName(name);
        return ResponseEntity.ok(tenants);
    }
    
    /**
     * Şehir ve ilçeye göre kuaför arama
     */
    @GetMapping("/by-location")
    @Operation(summary = "Konuma göre kuaför arama", description = "Şehir ve ilçeye göre kuaförleri listeler")
    public ResponseEntity<List<TenantDto>> getTenantsByLocation(
            @Parameter(description = "Şehir adı") @RequestParam String city,
            @Parameter(description = "İlçe adı (opsiyonel)") @RequestParam(required = false) String district) {
        List<TenantDto> tenants;
        if (district != null && !district.trim().isEmpty()) {
            tenants = tenantService.findByCityAndDistrictDto(city, district);
        } else {
            tenants = tenantService.findByCityDto(city);
        }
        return ResponseEntity.ok(tenants);
    }
    
    /**
     * Yeni kuaför oluştur
     */
    @PostMapping
    @Operation(summary = "Yeni kuaför oluştur", description = "Sisteme yeni kuaför ekler")
    public ResponseEntity<TenantDto> createTenant(
            @Parameter(description = "Kuaför bilgileri") @Valid @RequestBody CreateTenantRequest request) {
        try {
            TenantDto createdTenant = tenantService.createTenantWithServices(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTenant);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Kuaför güncelle
     */
    @PutMapping("/{id}")
    @Operation(summary = "Kuaför güncelle", description = "Mevcut kuaförün bilgilerini günceller")
    public ResponseEntity<TenantDto> updateTenant(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long id,
            @Parameter(description = "Güncellenecek kuaför bilgileri") @Valid @RequestBody TenantDto tenantDto) {
        try {
            TenantDto updatedTenant = tenantService.updateTenant(id, tenantDto);
            return ResponseEntity.ok(updatedTenant);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Kuaförü pasif hale getir
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Kuaförü pasif hale getir", description = "Kuaförü sistem üzerinde pasif hale getirir (soft delete)")
    public ResponseEntity<Void> deactivateTenant(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long id) {
        try {
            tenantService.deactivateTenant(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Kuaförü tekrar aktif hale getir
     */
    @PostMapping("/{id}/activate")
    @Operation(summary = "Kuaförü aktif hale getir", description = "Pasif durumdaki kuaförü tekrar aktif hale getirir")
    public ResponseEntity<Void> activateTenant(
            @Parameter(description = "Kuaför ID'si") @PathVariable Long id) {
        try {
            tenantService.activateTenant(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Aktif kuaför sayısı
     */
    @GetMapping("/count")
    @Operation(summary = "Aktif kuaför sayısı", description = "Sistemdeki toplam aktif kuaför sayısını getirir")
    public ResponseEntity<Long> getActiveTenantCount() {
        long count = tenantService.countActiveTenants();
        return ResponseEntity.ok(count);
    }
    
    /**
     * WhatsApp numarasına göre tenant ID bulma (internal API)
     */
    @GetMapping("/internal/tenant-id-by-whatsapp/{phoneNumber}")
    @Operation(summary = "WhatsApp numarasına göre tenant ID bul", 
               description = "Internal API - WhatsApp webhook için tenant ID bulur")
    public ResponseEntity<Long> getTenantIdByWhatsAppNumber(
            @Parameter(description = "WhatsApp telefon numarası") @PathVariable String phoneNumber) {
        Long tenantId = tenantService.findTenantIdByWhatsAppNumber(phoneNumber);
        if (tenantId != null) {
            return ResponseEntity.ok(tenantId);
        }
        return ResponseEntity.notFound().build();
    }
}
