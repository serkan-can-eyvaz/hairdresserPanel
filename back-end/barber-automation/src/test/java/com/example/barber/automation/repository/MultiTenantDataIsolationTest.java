package com.example.barber.automation.repository;

import com.example.barber.automation.TestDataBuilder;
import com.example.barber.automation.entity.*;
import com.example.barber.automation.entity.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Multi-Tenant Data Isolation Integration Test
 * 
 * Bu test sınıfı sistemin en kritik özelliğini test eder: MULTI-TENANT DATA ISOLATION
 * 
 * Test senaryoları:
 * - Her kuaför sadece kendi müşterilerini görebilir
 * - Her kuaför sadece kendi hizmetlerini görebilir  
 * - Her kuaför sadece kendi randevularını görebilir
 * - Her kuaför sadece kendi ayarlarını görebilir
 * - Cross-tenant veri sızıntısı olmamalı
 * - Query'ler tenant_id bazlı filtreleme yapmalı
 * - Entity relationship'ler tenant sınırlarını aşmamalı
 * 
 * Bu sistemin SaaS olarak satılabilmesi için bu testlerin %100 başarılı olması kritik!
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Multi-Tenant Data Isolation Integration Tests")
class MultiTenantDataIsolationTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private TenantRepository tenantRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private ServiceRepository serviceRepository;
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Autowired
    private TenantSettingRepository tenantSettingRepository;
    
    @Autowired
    private ReminderRepository reminderRepository;
    
    // Test data - 3 farklı kuaför ve onların verileri
    private Tenant kuaforA, kuaforB, kuaforC;
    private Customer customerA1, customerA2, customerB1, customerB2, customerC1;
    private Service serviceA1, serviceA2, serviceB1, serviceB2, serviceC1;
    private Appointment appointmentA1, appointmentA2, appointmentB1, appointmentC1;
    private TenantSetting settingA1, settingB1, settingC1;
    private Reminder reminderA1, reminderB1;
    
    @BeforeEach
    void setUp() {
        // 3 farklı kuaför oluştur
        kuaforA = TestDataBuilder.createTestTenant("Kuaför A", "+905321111111");
        kuaforB = TestDataBuilder.createTestTenant("Kuaför B", "+905322222222");  
        kuaforC = TestDataBuilder.createTestTenant("Kuaför C", "+905323333333");
        
        entityManager.persistAndFlush(kuaforA);
        entityManager.persistAndFlush(kuaforB);
        entityManager.persistAndFlush(kuaforC);
        
        // Her kuaför için müşteriler oluştur
        customerA1 = TestDataBuilder.createTestCustomer("Ali A", "+905331111111", kuaforA);
        customerA2 = TestDataBuilder.createTestCustomer("Ayşe A", "+905331111112", kuaforA);
        customerB1 = TestDataBuilder.createTestCustomer("Mehmet B", "+905332222221", kuaforB);
        customerB2 = TestDataBuilder.createTestCustomer("Fatma B", "+905332222222", kuaforB);
        customerC1 = TestDataBuilder.createTestCustomer("Can C", "+905333333331", kuaforC);
        
        entityManager.persistAndFlush(customerA1);
        entityManager.persistAndFlush(customerA2);
        entityManager.persistAndFlush(customerB1);
        entityManager.persistAndFlush(customerB2);
        entityManager.persistAndFlush(customerC1);
        
        // Her kuaför için hizmetler oluştur
        serviceA1 = TestDataBuilder.createTestService("Saç A", 45, new BigDecimal("100"), kuaforA);
        serviceA2 = TestDataBuilder.createTestService("Sakal A", 30, new BigDecimal("80"), kuaforA);
        serviceB1 = TestDataBuilder.createTestService("Saç B", 60, new BigDecimal("150"), kuaforB);
        serviceB2 = TestDataBuilder.createTestService("Sakal B", 45, new BigDecimal("120"), kuaforB);
        serviceC1 = TestDataBuilder.createTestService("Saç C", 30, new BigDecimal("200"), kuaforC);
        
        entityManager.persistAndFlush(serviceA1);
        entityManager.persistAndFlush(serviceA2);
        entityManager.persistAndFlush(serviceB1);
        entityManager.persistAndFlush(serviceB2);
        entityManager.persistAndFlush(serviceC1);
        
        // Her kuaför için randevular oluştur
        LocalDateTime now = LocalDateTime.now();
        appointmentA1 = TestDataBuilder.createTestAppointment(now.plusHours(1), customerA1, serviceA1, kuaforA);
        appointmentA2 = TestDataBuilder.createTestAppointment(now.plusHours(2), customerA2, serviceA2, kuaforA);
        appointmentB1 = TestDataBuilder.createTestAppointment(now.plusHours(3), customerB1, serviceB1, kuaforB);
        appointmentC1 = TestDataBuilder.createTestAppointment(now.plusHours(4), customerC1, serviceC1, kuaforC);
        
        entityManager.persistAndFlush(appointmentA1);
        entityManager.persistAndFlush(appointmentA2);
        entityManager.persistAndFlush(appointmentB1);
        entityManager.persistAndFlush(appointmentC1);
        
        // Her kuaför için ayarlar oluştur
        settingA1 = TestDataBuilder.createTestTenantSetting("test_setting", "value_A", TenantSetting.SettingType.STRING, kuaforA);
        settingB1 = TestDataBuilder.createTestTenantSetting("test_setting", "value_B", TenantSetting.SettingType.STRING, kuaforB);
        settingC1 = TestDataBuilder.createTestTenantSetting("test_setting", "value_C", TenantSetting.SettingType.STRING, kuaforC);
        
        entityManager.persistAndFlush(settingA1);
        entityManager.persistAndFlush(settingB1);
        entityManager.persistAndFlush(settingC1);
        
        // Her kuaför için hatırlatmalar oluştur
        reminderA1 = TestDataBuilder.createTestReminder(now.plusDays(30), Reminder.ReminderType.FOLLOW_UP, customerA1, kuaforA);
        reminderB1 = TestDataBuilder.createTestReminder(now.plusDays(30), Reminder.ReminderType.FOLLOW_UP, customerB1, kuaforB);
        
        entityManager.persistAndFlush(reminderA1);
        entityManager.persistAndFlush(reminderB1);
        
        entityManager.clear();
    }
    
    @Test
    @DisplayName("Customer Data Isolation - Kuaförler sadece kendi müşterilerini görür")
    void customerDataIsolation_ShouldOnlyShowOwnCustomers() {
        // When: Her kuaförün müşterilerini sorgula
        List<Customer> customersA = customerRepository.findByTenantIdAndActiveTrueOrderByNameAsc(kuaforA.getId());
        List<Customer> customersB = customerRepository.findByTenantIdAndActiveTrueOrderByNameAsc(kuaforB.getId());
        List<Customer> customersC = customerRepository.findByTenantIdAndActiveTrueOrderByNameAsc(kuaforC.getId());
        
        // Then: Her kuaför sadece kendi müşterilerini görür
        assertThat(customersA).hasSize(2);
        assertThat(customersB).hasSize(2);
        assertThat(customersC).hasSize(1);
        
        // Kuaför A sadece A müşterilerini görür
        assertThat(customersA)
                .extracting(Customer::getName)
                .containsExactlyInAnyOrder("Ali A", "Ayşe A");
        
        // Kuaför B sadece B müşterilerini görür  
        assertThat(customersB)
                .extracting(Customer::getName)
                .containsExactlyInAnyOrder("Mehmet B", "Fatma B");
        
        // Kuaför C sadece C müşterilerini görür
        assertThat(customersC)
                .extracting(Customer::getName)
                .containsExactly("Can C");
        
        // Cross-tenant contamination check
        assertThat(customersA)
                .extracting(customer -> customer.getTenant().getId())
                .containsOnly(kuaforA.getId());
        
        assertThat(customersB)
                .extracting(customer -> customer.getTenant().getId())
                .containsOnly(kuaforB.getId());
    }
    
    @Test
    @DisplayName("Service Data Isolation - Kuaförler sadece kendi hizmetlerini görür")
    void serviceDataIsolation_ShouldOnlyShowOwnServices() {
        // When: Her kuaförün hizmetlerini sorgula
        List<Service> servicesA = serviceRepository.findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(kuaforA.getId());
        List<Service> servicesB = serviceRepository.findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(kuaforB.getId());
        List<Service> servicesC = serviceRepository.findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(kuaforC.getId());
        
        // Then: Her kuaför sadece kendi hizmetlerini görür
        assertThat(servicesA).hasSize(2);
        assertThat(servicesB).hasSize(2);
        assertThat(servicesC).hasSize(1);
        
        // Kuaför A hizmetleri
        assertThat(servicesA)
                .extracting(Service::getName)
                .containsExactlyInAnyOrder("Saç A", "Sakal A");
        
        // Kuaför B hizmetleri
        assertThat(servicesB)
                .extracting(Service::getName)
                .containsExactlyInAnyOrder("Saç B", "Sakal B");
        
        // Kuaför C hizmetleri
        assertThat(servicesC)
                .extracting(Service::getName)
                .containsExactly("Saç C");
        
        // Fiyat bilgileri de izole olmalı
        assertThat(servicesA.get(0).getPrice()).isIn(new BigDecimal("100"), new BigDecimal("80"));
        assertThat(servicesB.get(0).getPrice()).isIn(new BigDecimal("150"), new BigDecimal("120"));
        assertThat(servicesC.get(0).getPrice()).isEqualTo(new BigDecimal("200"));
    }
    
    @Test
    @DisplayName("Appointment Data Isolation - Kuaförler sadece kendi randevularını görür")
    void appointmentDataIsolation_ShouldOnlyShowOwnAppointments() {
        // When: Her kuaförün randevularını sorgula
        List<Appointment> appointmentsA = appointmentRepository.findActiveAppointmentsByTenantId(kuaforA.getId());
        List<Appointment> appointmentsB = appointmentRepository.findActiveAppointmentsByTenantId(kuaforB.getId());
        List<Appointment> appointmentsC = appointmentRepository.findActiveAppointmentsByTenantId(kuaforC.getId());
        
        // Then: Her kuaför sadece kendi randevularını görür
        assertThat(appointmentsA).hasSize(2);
        assertThat(appointmentsB).hasSize(1);
        assertThat(appointmentsC).hasSize(1);
        
        // Randevulardaki müşteri ve hizmet bilgileri de doğru tenant'a ait olmalı
        for (Appointment appointment : appointmentsA) {
            assertThat(appointment.getTenant().getId()).isEqualTo(kuaforA.getId());
            assertThat(appointment.getCustomer().getTenant().getId()).isEqualTo(kuaforA.getId());
            assertThat(appointment.getService().getTenant().getId()).isEqualTo(kuaforA.getId());
        }
        
        for (Appointment appointment : appointmentsB) {
            assertThat(appointment.getTenant().getId()).isEqualTo(kuaforB.getId());
            assertThat(appointment.getCustomer().getTenant().getId()).isEqualTo(kuaforB.getId());
            assertThat(appointment.getService().getTenant().getId()).isEqualTo(kuaforB.getId());
        }
    }
    
    @Test
    @DisplayName("Settings Data Isolation - Kuaförler sadece kendi ayarlarını görür")
    void settingsDataIsolation_ShouldOnlyShowOwnSettings() {
        // When: Her kuaförün ayarlarını sorgula
        List<TenantSetting> settingsA = tenantSettingRepository.findByTenantIdOrderBySettingKeyAsc(kuaforA.getId());
        List<TenantSetting> settingsB = tenantSettingRepository.findByTenantIdOrderBySettingKeyAsc(kuaforB.getId());
        List<TenantSetting> settingsC = tenantSettingRepository.findByTenantIdOrderBySettingKeyAsc(kuaforC.getId());
        
        // Then: Her kuaför sadece kendi ayarlarını görür
        assertThat(settingsA).hasSize(1);
        assertThat(settingsB).hasSize(1);
        assertThat(settingsC).hasSize(1);
        
        // Ayar değerleri farklı olmalı
        assertThat(settingsA.get(0).getSettingValue()).isEqualTo("value_A");
        assertThat(settingsB.get(0).getSettingValue()).isEqualTo("value_B");
        assertThat(settingsC.get(0).getSettingValue()).isEqualTo("value_C");
    }
    
    @Test
    @DisplayName("Reminder Data Isolation - Kuaförler sadece kendi hatırlatmalarını görür")
    void reminderDataIsolation_ShouldOnlyShowOwnReminders() {
        // When: Her kuaförün hatırlatmalarını sorgula
        List<Reminder> remindersA = reminderRepository.findByTenantIdAndStatusOrderByScheduledForAsc(kuaforA.getId(), Reminder.ReminderStatus.PENDING);
        List<Reminder> remindersB = reminderRepository.findByTenantIdAndStatusOrderByScheduledForAsc(kuaforB.getId(), Reminder.ReminderStatus.PENDING);
        List<Reminder> remindersC = reminderRepository.findByTenantIdAndStatusOrderByScheduledForAsc(kuaforC.getId(), Reminder.ReminderStatus.PENDING);
        
        // Then: Her kuaför sadece kendi hatırlatmalarını görür
        assertThat(remindersA).hasSize(1);
        assertThat(remindersB).hasSize(1);
        assertThat(remindersC).hasSize(0); // C için hatırlatma oluşturmadık
        
        // Hatırlatmalardaki müşteri bilgileri doğru tenant'a ait olmalı
        assertThat(remindersA.get(0).getCustomer().getTenant().getId()).isEqualTo(kuaforA.getId());
        assertThat(remindersB.get(0).getCustomer().getTenant().getId()).isEqualTo(kuaforB.getId());
    }
    
    @Test
    @DisplayName("Cross-Tenant Query Injection Test - Güvenlik testi")
    void crossTenantQueryInjection_ShouldNotAllowDataLeakage() {
        // Given: Kötü niyetli tenant ID'ler
        Long maliciousTenantId = -1L; // Geçersiz ID
        Long anotherTenantId = kuaforB.getId(); // Başka kuaförün ID'si
        
        // When: Kuaför A'nın ID'si ile sorgu yapılır
        List<Customer> legitCustomers = customerRepository.findByTenantIdAndActiveTrueOrderByNameAsc(kuaforA.getId());
        List<Customer> maliciousCustomers = customerRepository.findByTenantIdAndActiveTrueOrderByNameAsc(maliciousTenantId);
        List<Customer> crossTenantCustomers = customerRepository.findByTenantIdAndActiveTrueOrderByNameAsc(anotherTenantId);
        
        // Then: Sadece doğru tenant'ın verileri döner
        assertThat(legitCustomers).hasSize(2); // Kuaför A'nın müşterileri
        assertThat(maliciousCustomers).hasSize(0); // Geçersiz ID için sonuç yok
        assertThat(crossTenantCustomers).hasSize(2); // Kuaför B'nin müşterileri (normal davranış)
        
        // Kuaför A sorgusu Kuaför B verilerini döndürmemeli
        assertThat(legitCustomers)
                .extracting(Customer::getName)
                .doesNotContain("Mehmet B", "Fatma B");
    }
    
    @Test
    @DisplayName("Entity Relationship Isolation - İlişkili entity'lerde veri sızıntısı kontrolü")
    void entityRelationshipIsolation_ShouldMaintainTenantBoundaries() {
        // When: Randevu sorgularken related entity'leri de yükle
        List<Appointment> appointmentsA = appointmentRepository.findActiveAppointmentsByTenantId(kuaforA.getId());
        
        // Then: İlişkili entity'ler de aynı tenant'a ait olmalı
        for (Appointment appointment : appointmentsA) {
            // Appointment tenant'ı
            assertThat(appointment.getTenant().getId()).isEqualTo(kuaforA.getId());
            
            // Customer tenant'ı
            assertThat(appointment.getCustomer().getTenant().getId()).isEqualTo(kuaforA.getId());
            
            // Service tenant'ı  
            assertThat(appointment.getService().getTenant().getId()).isEqualTo(kuaforA.getId());
            
            // Diğer tenant'ların entity'leri olmamalı
            assertThat(appointment.getCustomer().getName()).doesNotContain("B", "C");
            assertThat(appointment.getService().getName()).doesNotContain("B", "C");
        }
    }
    
    @Test
    @DisplayName("Data Count Consistency - Veri sayıları tutarlılık kontrolü")
    void dataCountConsistency_ShouldMaintainCorrectCounts() {
        // When: Her kuaför için veri sayılarını kontrol et
        long customersCountA = customerRepository.countByTenantIdAndActiveTrue(kuaforA.getId());
        long customersCountB = customerRepository.countByTenantIdAndActiveTrue(kuaforB.getId());
        long customersCountC = customerRepository.countByTenantIdAndActiveTrue(kuaforC.getId());
        
        long servicesCountA = serviceRepository.countByTenantIdAndActiveTrue(kuaforA.getId());
        long servicesCountB = serviceRepository.countByTenantIdAndActiveTrue(kuaforB.getId());
        long servicesCountC = serviceRepository.countByTenantIdAndActiveTrue(kuaforC.getId());
        
        // Then: Sayılar beklendiği gibi olmalı
        assertThat(customersCountA).isEqualTo(2);
        assertThat(customersCountB).isEqualTo(2);
        assertThat(customersCountC).isEqualTo(1);
        
        assertThat(servicesCountA).isEqualTo(2);
        assertThat(servicesCountB).isEqualTo(2);
        assertThat(servicesCountC).isEqualTo(1);
        
        // Toplam sayı kontrolü
        long totalCustomers = customersCountA + customersCountB + customersCountC;
        long totalServices = servicesCountA + servicesCountB + servicesCountC;
        
        assertThat(totalCustomers).isEqualTo(5); // 2+2+1
        assertThat(totalServices).isEqualTo(5);  // 2+2+1
    }
    
    @Test
    @DisplayName("Stress Test - Çok sayıda tenant ile data isolation")
    void stressTestDataIsolation_WithManyTenants_ShouldMaintainIsolation() {
        // Given: 50 farklı kuaför oluştur
        for (int i = 10; i < 60; i++) {
            Tenant tenant = TestDataBuilder.createTestTenant("Kuaför " + i, "+90532" + String.format("%07d", i));
            entityManager.persist(tenant);
            
            // Her kuaför için 3 müşteri
            for (int j = 1; j <= 3; j++) {
                Customer customer = TestDataBuilder.createTestCustomer("Müşteri " + i + "-" + j, "+90533" + String.format("%06d", i*10+j), tenant);
                entityManager.persist(customer);
            }
        }
        entityManager.flush();
        entityManager.clear();
        
        // When: Random bir kuaförün verilerini sorgula
        List<Tenant> allTenants = tenantRepository.findByActiveTrue();
        Tenant randomTenant = allTenants.get(25); // 25. kuaför
        
        List<Customer> randomTenantCustomers = customerRepository.findByTenantIdAndActiveTrueOrderByNameAsc(randomTenant.getId());
        
        // Then: Sadece o kuaförün müşterileri dönmeli
        assertThat(randomTenantCustomers).hasSize(3);
        assertThat(randomTenantCustomers)
                .extracting(customer -> customer.getTenant().getId())
                .containsOnly(randomTenant.getId());
        
        // Toplam tenant sayısı doğru olmalı (orijinal 3 + yeni 50 = 53)
        assertThat(allTenants).hasSizeGreaterThanOrEqualTo(53);
    }
}
