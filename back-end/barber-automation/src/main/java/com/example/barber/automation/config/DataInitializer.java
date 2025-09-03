package com.example.barber.automation.config;

import com.example.barber.automation.entity.Service;
import com.example.barber.automation.entity.Tenant;
import com.example.barber.automation.entity.TenantUser;
import com.example.barber.automation.repository.ServiceRepository;
import com.example.barber.automation.repository.TenantRepository;
import com.example.barber.automation.repository.TenantUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Uygulama başladığında gerekli verileri oluşturan sınıf
 * - Süper admin kullanıcısı
 * - Varsayılan tenant
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantUserRepository tenantUserRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        createSuperAdmin();
        createDefaultServices();
    }

    /**
     * Süper admin kullanıcısı ve varsayılan tenant oluşturur
     */
    private void createSuperAdmin() {
        // Varsayılan tenant oluştur
        Tenant defaultTenant = tenantRepository.findByName("Sistem Yönetimi")
                .orElseGet(() -> {
                    Tenant tenant = new Tenant();
                    tenant.setName("Sistem Yönetimi");
                    tenant.setDescription("Sistem yönetimi için varsayılan tenant");
                    tenant.setAddress("Sistem Adresi");
                    tenant.setCity("Ankara");
                    tenant.setDistrict("Çankaya");
                    tenant.setPhoneNumber("+905550000000");
                    tenant.setEmail("admin@system.com");
                    tenant.setActive(true);
                    tenant.setCreatedAt(LocalDateTime.now());
                    tenant.setUpdatedAt(LocalDateTime.now());
                    return tenantRepository.save(tenant);
                });

        // Süper admin kullanıcısı oluştur
        if (!tenantUserRepository.existsByUsername("superadmin")) {
            TenantUser superAdmin = new TenantUser();
            superAdmin.setUsername("superadmin");
            superAdmin.setPassword(passwordEncoder.encode("superadmin123"));
            superAdmin.setFirstName("Süper");
            superAdmin.setLastName("Admin");
            superAdmin.setEmail("superadmin@system.com");
            superAdmin.setPhone("+90 555 000 0000");
            superAdmin.setRole(TenantUser.UserRole.SUPER_ADMIN);
            superAdmin.setActive(true);
            superAdmin.setTenant(defaultTenant);
            superAdmin.setCreatedAt(LocalDateTime.now());
            superAdmin.setUpdatedAt(LocalDateTime.now());

            tenantUserRepository.save(superAdmin);

            System.out.println("✅ Süper Admin kullanıcısı oluşturuldu!");
            System.out.println("👤 Kullanıcı adı: superadmin");
            System.out.println("🔑 Şifre: superadmin123");
            System.out.println("🏢 Tenant: " + defaultTenant.getName());
        } else {
            System.out.println("ℹ️ Süper Admin kullanıcısı zaten mevcut");
        }
    }

    /**
     * Varsayılan hizmetleri oluşturur
     */
    private void createDefaultServices() {
        // Varsayılan tenant'ı bul
        Tenant defaultTenant = tenantRepository.findByName("Sistem Yönetimi").orElse(null);
        if (defaultTenant == null) {
            System.out.println("⚠️ Varsayılan tenant bulunamadı, hizmetler oluşturulamadı");
            return;
        }

        // Varsayılan hizmetleri oluştur
        createServiceIfNotExists("Saç Kesimi", "Profesyonel saç kesimi hizmeti", 30, 50.0, defaultTenant);
        createServiceIfNotExists("Sakal Tıraşı", "Profesyonel sakal tıraşı hizmeti", 20, 30.0, defaultTenant);
        createServiceIfNotExists("Saç Yıkama", "Saç yıkama ve bakım hizmeti", 15, 25.0, defaultTenant);
        createServiceIfNotExists("Fön", "Saç fön ve şekillendirme hizmeti", 25, 35.0, defaultTenant);
        createServiceIfNotExists("Saç ve Sakal", "Saç kesimi ve sakal tıraşı paketi", 45, 70.0, defaultTenant);

        System.out.println("✅ Varsayılan hizmetler oluşturuldu!");
    }

    private void createServiceIfNotExists(String name, String description, Integer durationMinutes, Double price, Tenant tenant) {
        if (!serviceRepository.existsByNameAndTenant(name, tenant)) {
            Service service = new Service();
            service.setName(name);
            service.setDescription(description);
            service.setDurationMinutes(durationMinutes);
            service.setPrice(java.math.BigDecimal.valueOf(price));
            service.setCurrency("TRY");
            service.setActive(true);
            service.setSortOrder(0);
            service.setTenant(tenant);
            service.setCreatedAt(LocalDateTime.now());
            service.setUpdatedAt(LocalDateTime.now());

            serviceRepository.save(service);
            System.out.println("✅ Hizmet oluşturuldu: " + name);
        }
    }
}
