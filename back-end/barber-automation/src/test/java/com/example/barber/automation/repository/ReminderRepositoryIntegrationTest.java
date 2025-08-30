package com.example.barber.automation.repository;

import com.example.barber.automation.TestDataBuilder;
import com.example.barber.automation.entity.*;
import com.example.barber.automation.entity.Reminder.ReminderStatus;
import com.example.barber.automation.entity.Reminder.ReminderType;
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
 * ReminderRepository Integration Tests
 * JPA query doğrulaması ve veritabanı işlemleri testleri
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ReminderRepository Integration Tests")
class ReminderRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReminderRepository reminderRepository;

    private Tenant tenant1, tenant2;
    private Customer customer1, customer2;
    private Appointment appointment1;

    @BeforeEach
    void setUp() {
        // Test data oluştur
        tenant1 = TestDataBuilder.createTestTenant("Kuaför A", "+905321112233");
        tenant2 = TestDataBuilder.createTestTenant("Kuaför B", "+905376667788");
        
        customer1 = TestDataBuilder.createTestCustomer("Ali Can", "+905311112222", tenant1);
        customer2 = TestDataBuilder.createTestCustomer("Ayşe Yılmaz", "+905344445555", tenant2);
        
        Service service1 = TestDataBuilder.createTestService("Saç Kesimi", 45, 
            new BigDecimal("100.0"), tenant1);
        
        entityManager.persistAndFlush(tenant1);
        entityManager.persistAndFlush(tenant2);
        entityManager.persistAndFlush(customer1);
        entityManager.persistAndFlush(customer2);
        entityManager.persistAndFlush(service1);
        
        appointment1 = TestDataBuilder.createTestAppointment(
            LocalDateTime.now().plusDays(1), customer1, service1, tenant1);
        entityManager.persistAndFlush(appointment1);
        
        entityManager.flush();
        entityManager.clear();
    }

    private Reminder createTestReminder(Tenant tenant, Customer customer, Appointment appointment, ReminderStatus status) {
        Reminder reminder = new Reminder();
        reminder.setTenant(tenant);
        reminder.setCustomer(customer);
        reminder.setAppointment(appointment);
        reminder.setReminderType(ReminderType.APPOINTMENT_REMINDER);
        reminder.setStatus(status);
        reminder.setMessage("Test hatırlatma mesajı");
        reminder.setScheduledAt(LocalDateTime.now().plusHours(1));
        
        if (status == ReminderStatus.SENT) {
            reminder.setSentAt(LocalDateTime.now());
        }
        
        return reminder;
    }

    @Test
    @DisplayName("Tenant'a göre pending reminder'ları listeleme")
    void findByTenantIdAndStatusOrderByScheduledAtAsc_ShouldReturnPendingReminders() {
        // Given
        Reminder pendingReminder = createTestReminder(tenant1, customer1, appointment1, ReminderStatus.PENDING);
        Reminder sentReminder = createTestReminder(tenant1, customer1, appointment1, ReminderStatus.SENT);
        
        entityManager.persistAndFlush(pendingReminder);
        entityManager.persistAndFlush(sentReminder);
        
        // When
        List<Reminder> result = reminderRepository.findByTenantIdAndStatusOrderByScheduledForAsc(tenant1.getId(), ReminderStatus.PENDING);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(result.get(0).getTenant().getId()).isEqualTo(tenant1.getId());
    }

    @Test
    @DisplayName("Belirli tarihten önce scheduled reminder'ları bulma")
    void findByTenantIdAndStatusAndScheduledAtBefore_ShouldReturnRemindersBefore() {
        // Given
        Reminder pastReminder = createTestReminder(tenant1, customer1, appointment1, ReminderStatus.PENDING);
        pastReminder.setScheduledAt(LocalDateTime.now().minusHours(2));
        entityManager.persistAndFlush(pastReminder);
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        
        // When
        List<Reminder> result = reminderRepository.findByTenantIdAndStatusAndScheduledForBefore(
            tenant1.getId(), ReminderStatus.PENDING, cutoffTime);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScheduledFor()).isBefore(cutoffTime);
    }

    @Test
    @DisplayName("Müşteri ID'ye göre reminder'ları listeleme")
    void findByTenantIdAndCustomerIdOrderByCreatedAtDesc_ShouldReturnCustomerReminders() {
        // Given
        Reminder reminder1 = createTestReminder(tenant1, customer1, appointment1, ReminderStatus.PENDING);
        entityManager.persistAndFlush(reminder1);
        
        // When
        List<Reminder> result = reminderRepository.findByTenantIdAndCustomerIdOrderByCreatedAtDesc(
            tenant1.getId(), customer1.getId());
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomer().getId()).isEqualTo(customer1.getId());
    }

    @Test
    @DisplayName("Appointment ID'ye göre reminder bulma")
    void findByAppointmentIdAndTenantId_ShouldReturnAppointmentReminders() {
        // Given
        Reminder reminder = createTestReminder(tenant1, customer1, appointment1, ReminderStatus.PENDING);
        entityManager.persistAndFlush(reminder);
        
        // When
        List<Reminder> result = reminderRepository.findByAppointmentIdAndTenantId(
            appointment1.getId(), tenant1.getId());
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAppointment().getId()).isEqualTo(appointment1.getId());
    }

    @Test
    @DisplayName("Tarih aralığında reminder'ları bulma")
    void findByTenantIdAndStatusAndScheduledAtBetween_ShouldReturnRemindersInRange() {
        // Given
        Reminder todayReminder = createTestReminder(tenant1, customer1, appointment1, ReminderStatus.PENDING);
        todayReminder.setScheduledAt(LocalDateTime.now());
        entityManager.persistAndFlush(todayReminder);
        
        LocalDateTime startDate = LocalDateTime.now().minusHours(1);
        LocalDateTime endDate = LocalDateTime.now().plusHours(1);
        
        // When
        List<Reminder> result = reminderRepository.findByTenantIdAndStatusAndScheduledForBetween(
            tenant1.getId(), ReminderStatus.PENDING, startDate, endDate);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScheduledFor()).isBetween(startDate, endDate);
    }

    @Test
    @DisplayName("Reminder type'a göre filtreleme")
    void findByTenantIdAndReminderType_ShouldReturnRemindersByType() {
        // Given
        Reminder appointmentReminder = createTestReminder(tenant1, customer1, appointment1, ReminderStatus.PENDING);
        appointmentReminder.setReminderType(ReminderType.APPOINTMENT_REMINDER);
        entityManager.persistAndFlush(appointmentReminder);
        
        Reminder followUpReminder = createTestReminder(tenant1, customer1, null, ReminderStatus.PENDING);
        followUpReminder.setReminderType(ReminderType.FOLLOW_UP);
        entityManager.persistAndFlush(followUpReminder);
        
        // When
        List<Reminder> result = reminderRepository.findByTenantIdAndType(
            tenant1.getId(), ReminderType.APPOINTMENT_REMINDER);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.stream())
            .allMatch(r -> r.getType() == ReminderType.APPOINTMENT_REMINDER);
        
        // When - follow up
        List<Reminder> followUpResult = reminderRepository.findByTenantIdAndType(
            tenant1.getId(), ReminderType.FOLLOW_UP);
        
        // Then
        assertThat(followUpResult).hasSize(1);
        assertThat(followUpResult.get(0).getType()).isEqualTo(ReminderType.FOLLOW_UP);
    }

    @Test
    @DisplayName("Data isolation - Tenant'lar arası veri izolasyonu")
    void testDataIsolation_ShouldNotReturnOtherTenantReminders() {
        // Given
        Reminder tenant1Reminder = createTestReminder(tenant1, customer1, appointment1, ReminderStatus.PENDING);
        Reminder tenant2Reminder = createTestReminder(tenant2, customer2, null, ReminderStatus.PENDING);
        
        entityManager.persistAndFlush(tenant1Reminder);
        entityManager.persistAndFlush(tenant2Reminder);
        
        // When
        List<Reminder> tenant1Results = reminderRepository.findByTenantIdAndStatusOrderByScheduledForAsc(
            tenant1.getId(), ReminderStatus.PENDING);
        List<Reminder> tenant2Results = reminderRepository.findByTenantIdAndStatusOrderByScheduledForAsc(
            tenant2.getId(), ReminderStatus.PENDING);
        
        // Then
        assertThat(tenant1Results).hasSize(1);
        assertThat(tenant2Results).hasSize(1);
        assertThat(tenant1Results.get(0).getTenant().getId()).isEqualTo(tenant1.getId());
        assertThat(tenant2Results.get(0).getTenant().getId()).isEqualTo(tenant2.getId());
    }

    @Test
    @DisplayName("Attempt count'a göre filtreleme")
    void findByTenantIdAndStatusAndAttemptCountLessThan_ShouldReturnLowAttemptReminders() {
        // Given
        Reminder failedReminder = createTestReminder(tenant1, customer1, appointment1, ReminderStatus.FAILED);
        failedReminder.setRetryCount(2);
        entityManager.persistAndFlush(failedReminder);
        
        // When
        List<Reminder> result = reminderRepository.findByTenantIdAndStatusAndRetryCountLessThan(
            tenant1.getId(), ReminderStatus.FAILED, 3);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRetryCount()).isLessThan(3);
    }

    @Test
    @DisplayName("Sent at tarihinden sonra gönderilmiş reminder'ları bulma")
    void findByTenantIdAndStatusAndSentAtAfter_ShouldReturnRecentlySentReminders() {
        // Given
        Reminder sentReminder = createTestReminder(tenant1, customer1, appointment1, ReminderStatus.SENT);
        sentReminder.setSentAt(LocalDateTime.now());
        entityManager.persistAndFlush(sentReminder);
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1);
        
        // When
        List<Reminder> result = reminderRepository.findByTenantIdAndStatusAndSentAtAfter(
            tenant1.getId(), ReminderStatus.SENT, cutoffTime);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSentAt()).isAfter(cutoffTime);
    }
}