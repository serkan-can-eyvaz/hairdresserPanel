package com.example.barber.automation.controller;

import com.example.barber.automation.dto.CreateTenantRequest;
import com.example.barber.automation.dto.CreateTenantSimpleRequest;
import com.example.barber.automation.dto.DashboardStats;
import com.example.barber.automation.dto.TenantDto;
import com.example.barber.automation.entity.Tenant;
import com.example.barber.automation.entity.TenantUser;
import com.example.barber.automation.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin panel işlemleri için controller
 * Sadece SUPER_ADMIN rolü erişebilir
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Admin panel işlemleri")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * Dashboard istatistikleri
     */
    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard istatistikleri", description = "Admin dashboard için genel istatistikleri döner")
    public ResponseEntity<DashboardStats> getDashboardStats() {
        try {
            DashboardStats stats = adminService.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Tüm tenant'ları listele (sayfalama ile)
     */
    @GetMapping("/tenants")
    @Operation(summary = "Tenant listesi", description = "Tüm kuaförleri sayfalama ile listeler")
    public ResponseEntity<List<TenantDto>> getAllTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Tenant> tenants = adminService.getAllTenants(pageable);
            
            // Tenant'ları DTO'ya çevir
            List<TenantDto> tenantDtos = tenants.getContent().stream()
                .map(tenant -> {
                    TenantDto dto = new TenantDto();
                    dto.setId(tenant.getId());
                    dto.setName(tenant.getName());
                    dto.setPhoneNumber(tenant.getPhoneNumber());
                    dto.setAddress(tenant.getAddress());
                    dto.setEmail(tenant.getEmail());
                    dto.setTimezone(tenant.getTimezone());
                    dto.setLogoUrl(tenant.getLogoUrl());
                    dto.setActive(tenant.getActive());
                    dto.setCreatedAt(tenant.getCreatedAt());
                    dto.setUpdatedAt(tenant.getUpdatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(tenantDtos);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Aktif tenant'ları listele
     */
    @GetMapping("/tenants/active")
    @Operation(summary = "Aktif tenant listesi", description = "Sadece aktif kuaförleri listeler")
    public ResponseEntity<List<Tenant>> getActiveTenants() {
        try {
            List<Tenant> tenants = adminService.getActiveTenants();
            return ResponseEntity.ok(tenants);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Tenant detayı
     */
    @GetMapping("/tenants/{tenantId}")
    @Operation(summary = "Tenant detayı", description = "Belirli bir kuaförün detaylarını döner")
    public ResponseEntity<?> getTenantById(@PathVariable Long tenantId) {
        try {
            Tenant tenant = adminService.getTenantById(tenantId);
            return ResponseEntity.ok(tenant);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Tenant bulunamadı: " + e.getMessage());
        }
    }

    /**
     * Yeni tenant oluştur (basit)
     */
    @PostMapping("/tenants/simple")
    @Operation(summary = "Yeni tenant oluştur (basit)", description = "Sadece kuaför oluşturur, admin kullanıcı oluşturmaz")
    public ResponseEntity<?> createTenantSimple(@Valid @RequestBody CreateTenantSimpleRequest request) {
        try {
            Tenant tenant = adminService.createTenantSimple(request);
            return ResponseEntity.ok(tenant);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Tenant oluşturulamadı: " + e.getMessage());
        }
    }

    /**
     * Yeni tenant oluştur
     */
    @PostMapping("/tenants")
    @Operation(summary = "Yeni tenant oluştur", description = "Yeni kuaför ve admin kullanıcı oluşturur")
    public ResponseEntity<?> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        try {
            Tenant tenant = adminService.createTenant(request);
            return ResponseEntity.ok(tenant);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Tenant oluşturulamadı: " + e.getMessage());
        }
    }

    /**
     * Tenant durumunu değiştir (aktif/pasif)
     */
    @PatchMapping("/tenants/{tenantId}/toggle-status")
    @Operation(summary = "Tenant durumu değiştir", description = "Kuaförü aktif/pasif yapar")
    public ResponseEntity<?> toggleTenantStatus(@PathVariable Long tenantId) {
        try {
            Tenant tenant = adminService.toggleTenantStatus(tenantId);
            return ResponseEntity.ok(tenant);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Tenant durumu değiştirilemedi: " + e.getMessage());
        }
    }

    /**
     * Tenant sil (soft delete)
     */
    @DeleteMapping("/tenants/{tenantId}")
    @Operation(summary = "Tenant sil", description = "Kuaförü ve kullanıcılarını pasifleştirir")
    public ResponseEntity<?> deleteTenant(@PathVariable Long tenantId) {
        try {
            adminService.deleteTenant(tenantId);
            return ResponseEntity.ok("Tenant başarıyla silindi");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Tenant silinemedi: " + e.getMessage());
        }
    }

    /**
     * Tenant kullanıcıları
     */
    @GetMapping("/tenants/{tenantId}/users")
    @Operation(summary = "Tenant kullanıcıları", description = "Belirli bir kuaförün kullanıcılarını listeler")
    public ResponseEntity<?> getTenantUsers(@PathVariable Long tenantId) {
        try {
            List<TenantUser> users = adminService.getTenantUsers(tenantId);
            
            // Password'leri response'tan çıkar
            users.forEach(user -> user.setPassword(null));
            
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Tenant kullanıcıları alınamadı: " + e.getMessage());
        }
    }

    /**
     * Tenant istatistikleri
     */
    @GetMapping("/tenants/{tenantId}/stats")
    @Operation(summary = "Tenant istatistikleri", description = "Belirli bir kuaförün istatistiklerini döner")
    public ResponseEntity<?> getTenantStats(@PathVariable Long tenantId) {
        try {
            DashboardStats stats = adminService.getTenantStats(tenantId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Tenant istatistikleri alınamadı: " + e.getMessage());
        }
    }
}
