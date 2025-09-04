package com.example.barber.automation.controller;

import com.example.barber.automation.dto.CreateServiceRequest;
import com.example.barber.automation.dto.ServiceDto;
import com.example.barber.automation.service.ServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Hizmet yönetimi için controller
 */
@RestController
@RequestMapping("/services")
@Tag(name = "Services", description = "Hizmet yönetimi")
@SecurityRequirement(name = "bearerAuth")
public class ServiceController {

    @Autowired
    private ServiceService serviceService;

    /**
     * Tenant'a ait tüm hizmetleri listele
     */
    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Tenant hizmetleri", description = "Belirli bir kuaförün hizmetlerini listeler")
    public ResponseEntity<List<ServiceDto>> getServicesByTenant(@PathVariable Long tenantId) {
        try {
            List<ServiceDto> services = serviceService.findAllByTenant(tenantId);
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Genel hizmet listesi (kuaför eklerken seçim için)
     */
    @GetMapping("/general")
    @Operation(summary = "Genel hizmet listesi", description = "Kuaför eklerken seçim yapılacak genel hizmet listesi")
    public ResponseEntity<List<ServiceDto>> getGeneralServices() {
        try {
            List<ServiceDto> services = serviceService.findAllByTenant(1L); // Tenant ID 1 = Sistem Yönetimi
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Hizmet adına göre arama
     */
    @GetMapping("/tenant/{tenantId}/search")
    @Operation(summary = "Hizmet arama", description = "Hizmet adına göre arama yapar")
    public ResponseEntity<List<ServiceDto>> searchServices(
            @PathVariable Long tenantId,
            @RequestParam String name) {
        try {
            List<ServiceDto> services = serviceService.searchByName(name, tenantId);
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Yeni hizmet oluştur
     */
    @PostMapping("/tenant/{tenantId}")
    @Operation(summary = "Hizmet oluştur", description = "Yeni hizmet ekler")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('TENANT_ADMIN') and #tenantId == authentication.principal.tenantId)")
    public ResponseEntity<?> createService(
            @PathVariable Long tenantId,
            @Valid @RequestBody CreateServiceRequest request) {
        try {
            // CreateServiceRequest'i ServiceDto'ya çevir
            ServiceDto serviceDto = new ServiceDto();
            serviceDto.setName(request.getName());
            serviceDto.setDescription(request.getDescription());
            serviceDto.setDurationMinutes(request.getDurationMinutes());
            serviceDto.setPrice(request.getPrice());
            serviceDto.setCurrency(request.getCurrency());
            serviceDto.setSortOrder(request.getSortOrder());
            
            ServiceDto createdService = serviceService.createService(serviceDto, tenantId);
            return ResponseEntity.ok(createdService);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Hizmet oluşturulamadı: " + e.getMessage());
        }
    }

    /**
     * Hizmet güncelle
     */
    @PutMapping("/{id}")
    @Operation(summary = "Hizmet güncelle", description = "Mevcut hizmeti günceller")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('TENANT_ADMIN') and @serviceService.isServiceOwner(#id, authentication.principal.tenantId))")
    public ResponseEntity<?> updateService(
            @PathVariable Long id,
            @Valid @RequestBody CreateServiceRequest request) {
        try {
            // CreateServiceRequest'i ServiceDto'ya çevir
            ServiceDto serviceDto = new ServiceDto();
            serviceDto.setId(id);
            serviceDto.setName(request.getName());
            serviceDto.setDescription(request.getDescription());
            serviceDto.setDurationMinutes(request.getDurationMinutes());
            serviceDto.setPrice(request.getPrice());
            serviceDto.setCurrency(request.getCurrency());
            serviceDto.setSortOrder(request.getSortOrder());
            
            ServiceDto updatedService = serviceService.updateService(id, serviceDto);
            return ResponseEntity.ok(updatedService);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Hizmet güncellenemedi: " + e.getMessage());
        }
    }

    /**
     * Hizmet sil (soft delete)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Hizmet sil", description = "Hizmeti pasifleştirir")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('TENANT_ADMIN') and @serviceService.isServiceOwner(#id, authentication.principal.tenantId))")
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        try {
            serviceService.deleteService(id);
            return ResponseEntity.ok("Hizmet başarıyla silindi");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Hizmet silinemedi: " + e.getMessage());
        }
    }

    /**
     * Hizmet durumunu değiştir (aktif/pasif)
     */
    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Hizmet durumu değiştir", description = "Hizmeti aktif/pasif yapar")
    @PreAuthorize("hasRole('SUPER_ADMIN') or (hasRole('TENANT_ADMIN') and @serviceService.isServiceOwner(#id, authentication.principal.tenantId))")
    public ResponseEntity<?> toggleServiceStatus(@PathVariable Long id) {
        try {
            ServiceDto service = serviceService.toggleServiceStatus(id);
            return ResponseEntity.ok(service);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Hizmet durumu değiştirilemedi: " + e.getMessage());
        }
    }

    /**
     * Hizmet detayı
     */
    @GetMapping("/{id}")
    @Operation(summary = "Hizmet detayı", description = "Belirli bir hizmetin detaylarını döner")
    public ResponseEntity<?> getServiceById(@PathVariable Long id) {
        try {
            // Tenant ID'yi request'ten al (şimdilik null, sonra düzelteceğiz)
            ServiceDto service = serviceService.findById(id, null).orElse(null);
            if (service != null) {
                return ResponseEntity.ok(service);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
