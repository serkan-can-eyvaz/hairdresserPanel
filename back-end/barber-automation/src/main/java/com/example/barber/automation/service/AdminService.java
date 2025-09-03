package com.example.barber.automation.service;

import com.example.barber.automation.dto.CreateTenantRequest;
import com.example.barber.automation.dto.CreateTenantSimpleRequest;
import com.example.barber.automation.dto.DashboardStats;
import com.example.barber.automation.entity.Tenant;
import com.example.barber.automation.entity.TenantUser;
import com.example.barber.automation.repository.AppointmentRepository;
import com.example.barber.automation.repository.CustomerRepository;
import com.example.barber.automation.repository.TenantRepository;
import com.example.barber.automation.repository.TenantUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Admin panel işlemleri için service sınıfı
 */
@Service
public class AdminService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantUserRepository tenantUserRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Dashboard istatistiklerini getir
     */
    public DashboardStats getDashboardStats() {
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByActiveTrue();
        long totalCustomers = customerRepository.count();
        long totalAppointments = appointmentRepository.count();

        // Bugünkü randevular
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        long todayAppointments = appointmentRepository.countByStartTimeBetween(startOfDay, endOfDay);

        // Bu ayki randevular
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);
        long monthlyAppointments = appointmentRepository.countByStartTimeBetween(startOfMonth, endOfMonth);

        return new DashboardStats(
                totalTenants,
                activeTenants,
                totalCustomers,
                totalAppointments,
                todayAppointments,
                monthlyAppointments
        );
    }

    /**
     * Tüm tenant'ları listele (sayfalama ile)
     */
    public Page<Tenant> getAllTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable);
    }

    /**
     * Aktif tenant'ları listele
     */
    public List<Tenant> getActiveTenants() {
        return tenantRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    /**
     * Tenant ID'ye göre tenant getir
     */
    public Tenant getTenantById(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant bulunamadı: " + tenantId));
    }

    /**
     * Yeni tenant oluştur (kuaför ve admin kullanıcı ile birlikte)
     */
    @Transactional
    public Tenant createTenant(CreateTenantRequest request) {
        // Telefon numarası kontrolü
        if (tenantRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Bu telefon numarası zaten kullanılıyor");
        }

        // Tenant oluştur
        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setPhoneNumber(request.getPhoneNumber());
        tenant.setAddress(request.getAddress());
        tenant.setEmail(request.getEmail());
        tenant.setTimezone(request.getTimezone());
        tenant.setActive(true);

        // Konum alanları
        tenant.setCity(request.getCity());
        tenant.setDistrict(request.getDistrict());
        tenant.setNeighborhood(request.getNeighborhood());
        tenant.setAddressDetail(request.getAddressDetail());

        Tenant savedTenant = tenantRepository.save(tenant);

        return savedTenant;
    }

    /**
     * Yeni tenant oluştur (sadece kuaför, admin kullanıcı olmadan)
     */
    @Transactional
    public Tenant createTenantSimple(CreateTenantSimpleRequest request) {
        // Telefon numarası kontrolü
        if (tenantRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Bu telefon numarası zaten kullanılıyor");
        }

        // Tenant oluştur
        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setPhoneNumber(request.getPhoneNumber());
        tenant.setAddress(request.getAddress());
        tenant.setEmail(request.getEmail());
        tenant.setTimezone(request.getTimezone());
        tenant.setActive(true);

        return tenantRepository.save(tenant);
    }

    /**
     * Tenant'ı aktif/pasif yap
     */
    @Transactional
    public Tenant toggleTenantStatus(Long tenantId) {
        Tenant tenant = getTenantById(tenantId);
        tenant.setActive(!tenant.getActive());
        return tenantRepository.save(tenant);
    }

    /**
     * Tenant'ı sil (soft delete)
     */
    @Transactional
    public void deleteTenant(Long tenantId) {
        Tenant tenant = getTenantById(tenantId);
        tenant.setActive(false);
        tenantRepository.save(tenant);

        // Tenant'a ait kullanıcıları da pasifleştir
        List<TenantUser> users = tenantUserRepository.findByTenantIdAndActiveTrue(tenantId);
        users.forEach(user -> user.setActive(false));
        tenantUserRepository.saveAll(users);
    }

    /**
     * Tenant'a ait kullanıcıları listele
     */
    public List<TenantUser> getTenantUsers(Long tenantId) {
        return tenantUserRepository.findByTenantIdAndActiveTrueOrderByCreatedAtDesc(tenantId);
    }

    /**
     * Tenant istatistiklerini getir
     */
    public DashboardStats getTenantStats(Long tenantId) {
        long customerCount = customerRepository.countByTenantIdAndActiveTrue(tenantId);
        long appointmentCount = appointmentRepository.countByTenantId(tenantId);

        // Bugünkü randevular
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        long todayAppointments = appointmentRepository.countByTenantIdAndStartTimeBetween(
                tenantId, startOfDay, endOfDay);

        // Bu ayki randevular
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);
        long monthlyAppointments = appointmentRepository.countByTenantIdAndStartTimeBetween(
                tenantId, startOfMonth, endOfMonth);

        DashboardStats stats = new DashboardStats();
        stats.setTotalTenants(1); // Bu tenant için
        stats.setActiveTenants(1);
        stats.setTotalCustomers(customerCount);
        stats.setTotalAppointments(appointmentCount);
        stats.setTodayAppointments(todayAppointments);
        stats.setMonthlyAppointments(monthlyAppointments);

        return stats;
    }
}
