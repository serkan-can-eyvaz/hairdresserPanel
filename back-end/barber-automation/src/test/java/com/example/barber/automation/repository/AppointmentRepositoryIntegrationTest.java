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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AppointmentRepository Integration Test
 * 
 * Bu test sınıfı randevu repository'sinin en kritik özelliklerini test eder:
 * - Multi-tenant data isolation (her kuaför sadece kendi randevularını görür)
 * - Tarih bazlı sorgular (günlük, haftalık, aylık)
 * - Randevu çakışma kontrolü (kritik business logic)
 * - Status bazlı filtreleme
 * - Complex join query'ler (appointment + customer + service + tenant)
 * - Performance kritik sorgular
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("AppointmentRepository Integration Tests")
class AppointmentRepositoryIntegrationTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    // Test entities
    private TestDataBuilder.MultiTenantTestData testData;
    private Appointment appointment1Tenant1; // Bugün 10:00-10:45
    private Appointment appointment2Tenant1; // Bugün 14:00-14:30  
    private Appointment appointment1Tenant2; // Bugün 11:00-11:45
    private Appointment appointmentYesterday; // Dün - completed
    private Appointment appointmentTomorrow; // Yarın - confirmed
    
    @BeforeEach
    void setUp() {
        // Multi-tenant test verisini oluştur
        testData = new TestDataBuilder.MultiTenantTestData();
        
        // Tenant'ları persist et
        entityManager.persistAndFlush(testData.tenant1);
        entityManager.persistAndFlush(testData.tenant2);
        
        // Customer'ları persist et
        entityManager.persistAndFlush(testData.customer1Tenant1);
        entityManager.persistAndFlush(testData.customer2Tenant1);
        entityManager.persistAndFlush(testData.customer1Tenant2);
        
        // Service'leri persist et
        entityManager.persistAndFlush(testData.service1Tenant1);
        entityManager.persistAndFlush(testData.service1Tenant2);
        
        // Randevuları oluştur ve persist et
        LocalDateTime today10AM = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime today2PM = LocalDateTime.now().withHour(14).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime today11AM = LocalDateTime.now().withHour(11).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1).withHour(15).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        
        // Tenant 1 randevuları
        appointment1Tenant1 = TestDataBuilder.createTestAppointment(today10AM, testData.customer1Tenant1, testData.service1Tenant1, testData.tenant1);
        appointment1Tenant1.setStatus(Appointment.AppointmentStatus.CONFIRMED);
        
        appointment2Tenant1 = TestDataBuilder.createTestAppointment(today2PM, testData.customer2Tenant1, testData.service1Tenant1, testData.tenant1);
        appointment2Tenant1.setStatus(Appointment.AppointmentStatus.PENDING);
        
        // Tenant 2 randevusu
        appointment1Tenant2 = TestDataBuilder.createTestAppointment(today11AM, testData.customer1Tenant2, testData.service1Tenant2, testData.tenant2);
        appointment1Tenant2.setStatus(Appointment.AppointmentStatus.CONFIRMED);
        
        // Geçmiş randevu (completed)
        appointmentYesterday = TestDataBuilder.createTestAppointment(yesterday, testData.customer1Tenant1, testData.service1Tenant1, testData.tenant1);
        appointmentYesterday.setStatus(Appointment.AppointmentStatus.COMPLETED);
        
        // Gelecek randevu (confirmed)
        appointmentTomorrow = TestDataBuilder.createTestAppointment(tomorrow, testData.customer1Tenant1, testData.service1Tenant1, testData.tenant1);
        appointmentTomorrow.setStatus(Appointment.AppointmentStatus.CONFIRMED);
        
        // Persist all appointments
        entityManager.persistAndFlush(appointment1Tenant1);
        entityManager.persistAndFlush(appointment2Tenant1);
        entityManager.persistAndFlush(appointment1Tenant2);
        entityManager.persistAndFlush(appointmentYesterday);
        entityManager.persistAndFlush(appointmentTomorrow);
        
        entityManager.clear();
    }
    
    @Test
    @DisplayName("Multi-tenant isolation - Her kuaför sadece kendi randevularını görür")
    void findByTenantId_ShouldIsolateDataByTenant() {
        // When: Her tenant için aktif randevuları sorgula
        List<Appointment> tenant1Appointments = appointmentRepository.findActiveAppointmentsByTenantId(testData.tenant1.getId());
        List<Appointment> tenant2Appointments = appointmentRepository.findActiveAppointmentsByTenantId(testData.tenant2.getId());
        
        // Then: Her tenant sadece kendi randevularını görür
        assertThat(tenant1Appointments).hasSize(3); // 2 bugün + 1 yarın
        assertThat(tenant2Appointments).hasSize(1); // 1 bugün
        
        // Tenant 1'in randevularında tenant 2'nin randevusu olmamalı
        assertThat(tenant1Appointments)
                .extracting(appointment -> appointment.getTenant().getId())
                .containsOnly(testData.tenant1.getId());
        
        // Tenant 2'nin randevularında tenant 1'in randevusu olmamalı
        assertThat(tenant2Appointments)
                .extracting(appointment -> appointment.getTenant().getId())
                .containsOnly(testData.tenant2.getId());
    }
    
    @Test
    @DisplayName("Tarih aralığı sorgusu - Belirli günler arası randevular")
    void findByTenantIdAndDateRange_ShouldReturnAppointmentsInRange() {
        // Given: Bugünün başı ve sonu
        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfToday = LocalDateTime.now().toLocalDate().atTime(23, 59, 59);
        
        // When: Bugünkü randevuları sorgula
        List<Appointment> todayAppointments = appointmentRepository.findByTenantIdAndDateRange(
                testData.tenant1.getId(), startOfToday, endOfToday);
        
        // Then: Sadece bugünkü randevular döner
        assertThat(todayAppointments).hasSize(2); // 10:00 ve 14:00 randevuları
        assertThat(todayAppointments)
                .extracting(Appointment::getStartTime)
                .allMatch(startTime -> startTime.toLocalDate().equals(LocalDateTime.now().toLocalDate()));
    }
    
    @Test
    @DisplayName("Randevu çakışma kontrolü - Kritik business logic")
    void findConflictingAppointments_ShouldDetectTimeConflicts() {
        // Given: Mevcut randevuyla çakışan zaman aralığı
        LocalDateTime conflictStart = LocalDateTime.now().withHour(10).withMinute(15).withSecond(0).withNano(0); // 10:15
        LocalDateTime conflictEnd = LocalDateTime.now().withHour(11).withMinute(0).withSecond(0).withNano(0);   // 11:00
        
        // When: Çakışan randevuları sorgula
        List<Appointment> conflictingAppointments = appointmentRepository.findConflictingAppointments(
                testData.tenant1.getId(), conflictStart, conflictEnd);
        
        // Then: Çakışan randevu bulunur (10:00-10:45 randevusu ile çakışır)
        assertThat(conflictingAppointments).hasSize(1);
        assertThat(conflictingAppointments.get(0).getStartTime().getHour()).isEqualTo(10);
    }
    
    @Test
    @DisplayName("Çakışma kontrolü - Çakışmayan zaman dilimi")
    void findConflictingAppointments_WhenNoConflict_ShouldReturnEmpty() {
        // Given: Hiçbir randevuyla çakışmayan zaman aralığı
        LocalDateTime noConflictStart = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0); // 12:00
        LocalDateTime noConflictEnd = LocalDateTime.now().withHour(13).withMinute(0).withSecond(0).withNano(0);   // 13:00
        
        // When: Çakışan randevuları sorgula
        List<Appointment> conflictingAppointments = appointmentRepository.findConflictingAppointments(
                testData.tenant1.getId(), noConflictStart, noConflictEnd);
        
        // Then: Çakışan randevu bulunmaz
        assertThat(conflictingAppointments).isEmpty();
    }
    
    @Test
    @DisplayName("Müşteri bazlı randevu sorgulama")
    void findByTenantIdAndCustomerId_ShouldReturnCustomerAppointments() {
        // When: Belirli müşterinin randevularını sorgula
        List<Appointment> customerAppointments = appointmentRepository.findByTenantIdAndCustomerId(
                testData.tenant1.getId(), testData.customer1Tenant1.getId());
        
        // Then: O müşterinin tüm randevuları döner
        assertThat(customerAppointments).hasSize(3); // Dün, bugün, yarın
        assertThat(customerAppointments)
                .extracting(appointment -> appointment.getCustomer().getId())
                .containsOnly(testData.customer1Tenant1.getId());
    }
    
    @Test
    @DisplayName("Status bazlı randevu sayma")
    void countByTenantIdAndStatus_ShouldReturnCorrectCounts() {
        // When: Farklı status'lerdeki randevu sayılarını sorgula
        long pendingCount = appointmentRepository.countByTenantIdAndStatus(testData.tenant1.getId(), Appointment.AppointmentStatus.PENDING);
        long confirmedCount = appointmentRepository.countByTenantIdAndStatus(testData.tenant1.getId(), Appointment.AppointmentStatus.CONFIRMED);
        long completedCount = appointmentRepository.countByTenantIdAndStatus(testData.tenant1.getId(), Appointment.AppointmentStatus.COMPLETED);
        
        // Then: Doğru sayılar döner
        assertThat(pendingCount).isEqualTo(1);  // 1 pending
        assertThat(confirmedCount).isEqualTo(2); // 2 confirmed (bugün + yarın)
        assertThat(completedCount).isEqualTo(1); // 1 completed (dün)
    }
    
    @Test
    @DisplayName("Bugünkü randevular")
    void findTodayAppointments_ShouldReturnTodayOnly() {
        // When: Bugünkü randevuları sorgula
        List<Appointment> todayAppointments = appointmentRepository.findTodayAppointments(testData.tenant1.getId());
        
        // Then: Sadece bugünkü randevular döner
        assertThat(todayAppointments).hasSize(2);
        assertThat(todayAppointments)
                .extracting(Appointment::getStartTime)
                .allMatch(startTime -> startTime.toLocalDate().equals(LocalDateTime.now().toLocalDate()));
    }
    
    @Test
    @DisplayName("Yaklaşan randevular (gelecek 7 gün)")
    void findUpcomingAppointments_ShouldReturnNext7Days() {
        // Given: Test için gelecek haftaya randevu ekle
        LocalDateTime nextWeek = LocalDateTime.now().plusDays(3).withHour(15).withMinute(0).withSecond(0).withNano(0);
        Appointment futureAppointment = TestDataBuilder.createTestAppointment(nextWeek, testData.customer1Tenant1, testData.service1Tenant1, testData.tenant1);
        futureAppointment.setStatus(Appointment.AppointmentStatus.CONFIRMED);
        entityManager.persistAndFlush(futureAppointment);
        
        // When: Yaklaşan randevuları sorgula
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekFromNow = now.plusDays(7);
        List<Appointment> upcomingAppointments = appointmentRepository.findUpcomingAppointments(testData.tenant1.getId(), now, weekFromNow);
        
        // Then: Gelecek 7 gün içindeki randevular döner (bugünkü randevular dahil değil)  
        assertThat(upcomingAppointments).hasSize(3); // yarın (1) + 3 gün sonra (1) + setup'taki ek gelecek randevu (1)
        assertThat(upcomingAppointments)
                .extracting(Appointment::getStartTime)
                .allMatch(startTime -> startTime.isAfter(now) && startTime.isBefore(weekFromNow));
    }
    
    @Test
    @DisplayName("Müşterinin aktif randevu kontrolü")
    void hasActiveAppointment_ShouldDetectActiveAppointments() {
        // When: Müşterinin aktif randevusu olup olmadığını kontrol et
        boolean hasActive = appointmentRepository.hasActiveAppointment(testData.tenant1.getId(), testData.customer1Tenant1.getId());
        
        // Then: Aktif randevusu olduğu tespit edilir
        assertThat(hasActive).isTrue();
    }
    
    @Test
    @DisplayName("Müşterinin son tamamlanan randevusu")
    void findLastCompletedAppointment_ShouldReturnMostRecent() {
        // When: Müşterinin son tamamlanan randevusunu sorgula
        Optional<Appointment> lastCompleted = appointmentRepository.findLastCompletedAppointment(
                testData.tenant1.getId(), testData.customer1Tenant1.getId());
        
        // Then: Dünkü completed randevu döner
        assertThat(lastCompleted).isPresent();
        assertThat(lastCompleted.get().getStatus()).isEqualTo(Appointment.AppointmentStatus.COMPLETED);
        assertThat(lastCompleted.get().getStartTime().toLocalDate()).isEqualTo(LocalDateTime.now().minusDays(1).toLocalDate());
    }
    
    @Test
    @DisplayName("Kaçırılan randevular - No show detection")
    void findMissedAppointments_ShouldDetectNoShows() {
        // Given: 2 saat önce başlayan ama hala pending olan randevu
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        Appointment missedAppointment = TestDataBuilder.createTestAppointment(twoHoursAgo, testData.customer1Tenant1, testData.service1Tenant1, testData.tenant1);
        missedAppointment.setStatus(Appointment.AppointmentStatus.PENDING);
        entityManager.persistAndFlush(missedAppointment);
        
        // When: Kaçırılan randevuları sorgula
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<Appointment> missedAppointments = appointmentRepository.findMissedAppointments(oneHourAgo);
        
        // Then: Kaçırılan randevu tespit edilir
        assertThat(missedAppointments).hasSizeGreaterThanOrEqualTo(1);
        assertThat(missedAppointments)
                .anyMatch(appointment -> appointment.getStartTime().equals(twoHoursAgo));
    }
    
    @Test
    @DisplayName("Performans testi - Büyük veri setiyle sorgulama")
    void findByDateRange_WithLargeDataset_ShouldPerformWell() {
        // Given: Çok sayıda randevu oluştur
        for (int i = 0; i < 100; i++) {
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(i % 30).withHour(9 + (i % 8)).withMinute(0);
            Appointment appointment = TestDataBuilder.createTestAppointment(appointmentTime, testData.customer1Tenant1, testData.service1Tenant1, testData.tenant1);
            appointment.setStatus(Appointment.AppointmentStatus.CONFIRMED);
            entityManager.persist(appointment);
        }
        entityManager.flush();
        entityManager.clear();
        
        // When: Geniş tarih aralığında sorgulama yap
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(30);
        
        long startTime = System.currentTimeMillis();
        List<Appointment> appointments = appointmentRepository.findByTenantIdAndDateRange(testData.tenant1.getId(), start, end);
        long endTime = System.currentTimeMillis();
        
        // Then: Sonuç doğru ve performans kabul edilebilir
        assertThat(appointments).hasSizeGreaterThan(50); // Çok sayıda randevu
        assertThat(endTime - startTime).isLessThan(1000); // 1 saniyeden az
        
        // Multi-tenant isolation hala korunuyor mu?
        assertThat(appointments)
                .extracting(appointment -> appointment.getTenant().getId())
                .containsOnly(testData.tenant1.getId());
    }
    
    @Test
    @DisplayName("Complex join query test - Customer ve Service bilgileriyle")
    void findAppointments_ShouldLoadRelatedEntities() {
        // When: Randevuları related entity'lerle beraber sorgula
        List<Appointment> appointments = appointmentRepository.findActiveAppointmentsByTenantId(testData.tenant1.getId());
        
        // Then: Related entity'ler lazy loading olmadan yüklenir
        assertThat(appointments).isNotEmpty();
        
        for (Appointment appointment : appointments) {
            // Customer bilgileri yüklü olmalı
            assertThat(appointment.getCustomer()).isNotNull();
            assertThat(appointment.getCustomer().getName()).isNotNull();
            
            // Service bilgileri yüklü olmalı
            assertThat(appointment.getService()).isNotNull();
            assertThat(appointment.getService().getName()).isNotNull();
            
            // Tenant bilgileri yüklü olmalı
            assertThat(appointment.getTenant()).isNotNull();
            assertThat(appointment.getTenant().getId()).isEqualTo(testData.tenant1.getId());
        }
    }
}
