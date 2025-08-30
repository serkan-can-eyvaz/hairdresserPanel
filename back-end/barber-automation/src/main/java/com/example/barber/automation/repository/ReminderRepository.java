package com.example.barber.automation.repository;

import com.example.barber.automation.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Reminder (Hatırlatma) Repository
 */
@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    
    /**
     * Tenant'a ait belirli bir hatırlatmayı bulma
     */
    Optional<Reminder> findByIdAndTenantId(Long id, Long tenantId);
    
    /**
     * Gönderilmeye hazır hatırlatmaları getirme
     */
    @Query("SELECT r FROM Reminder r WHERE r.status = 'PENDING' AND r.scheduledFor <= :now ORDER BY r.scheduledFor ASC")
    List<Reminder> findReadyToSendReminders(@Param("now") LocalDateTime now);
    
    /**
     * Tenant'a ait bekleyen hatırlatmaları getirme
     */
    List<Reminder> findByTenantIdAndStatusOrderByScheduledForAsc(Long tenantId, Reminder.ReminderStatus status);
    
    /**
     * Müşteriye ait hatırlatmaları getirme
     */
    @Query("SELECT r FROM Reminder r WHERE r.tenant.id = :tenantId AND r.customer.id = :customerId ORDER BY r.scheduledFor DESC")
    List<Reminder> findByTenantIdAndCustomerId(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);
    
    /**
     * Belirli tipte hatırlatmaları getirme
     */
    List<Reminder> findByTenantIdAndTypeAndStatusOrderByScheduledForAsc(Long tenantId, Reminder.ReminderType type, Reminder.ReminderStatus status);
    
    /**
     * Müşteri için aynı tipte bekleyen hatırlatma olup olmadığını kontrol etme
     */
    @Query("SELECT COUNT(r) > 0 FROM Reminder r WHERE r.tenant.id = :tenantId AND r.customer.id = :customerId AND r.type = :type AND r.status = 'PENDING'")
    boolean hasPendingReminderForCustomer(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId, @Param("type") Reminder.ReminderType type);
    
    /**
     * Başarısız hatırlatmaları getirme (tekrar deneme için)
     */
    @Query("SELECT r FROM Reminder r WHERE r.status = 'FAILED' AND r.retryCount < 3 AND r.scheduledFor <= :now ORDER BY r.scheduledFor ASC")
    List<Reminder> findFailedRemindersForRetry(@Param("now") LocalDateTime now);
    
    /**
     * Belirli tarih aralığındaki hatırlatmaları getirme
     */
    @Query("SELECT r FROM Reminder r WHERE r.tenant.id = :tenantId AND r.scheduledFor BETWEEN :startDate AND :endDate ORDER BY r.scheduledFor ASC")
    List<Reminder> findByTenantIdAndDateRange(@Param("tenantId") Long tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Randevuya bağlı hatırlatmaları getirme
     */
    @Query("SELECT r FROM Reminder r WHERE r.tenant.id = :tenantId AND r.appointment.id = :appointmentId")
    List<Reminder> findByTenantIdAndAppointmentId(@Param("tenantId") Long tenantId, @Param("appointmentId") Long appointmentId);
    
    /**
     * Eski hatırlatmaları temizleme (30 günden eski)
     */
    @Query("SELECT r FROM Reminder r WHERE r.status IN ('SENT', 'FAILED') AND r.scheduledFor < :cutoffDate")
    List<Reminder> findOldRemindersForCleanup(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Tenant'a ait hatırlatma istatistikleri
     */
    @Query("SELECT r.status, COUNT(r) FROM Reminder r WHERE r.tenant.id = :tenantId GROUP BY r.status")
    List<Object[]> getReminderStatsByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Günlük gönderilen hatırlatma sayısı
     */
    @Query("SELECT COUNT(r) FROM Reminder r WHERE r.tenant.id = :tenantId AND r.status = 'SENT' AND CAST(r.sentAt AS DATE) = CURRENT_DATE()")
    long countTodaySentRemindersByTenantId(@Param("tenantId") Long tenantId);
    
    // Test'ler için eksik method'lar
    
    /**
     * Tenant, status ve belirli tarihten önce
     */
    List<Reminder> findByTenantIdAndStatusAndScheduledForBefore(Long tenantId, Reminder.ReminderStatus status, LocalDateTime scheduledForBefore);
    
    /**
     * Tenant ve müşteri id'sine göre - en yeni önce
     */
    List<Reminder> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(Long tenantId, Long customerId);
    
    /**
     * Appointment id ve tenant id'ye göre
     */
    List<Reminder> findByAppointmentIdAndTenantId(Long appointmentId, Long tenantId);
    
    /**
     * Tenant, status ve tarih aralığı 
     */
    List<Reminder> findByTenantIdAndStatusAndScheduledForBetween(Long tenantId, Reminder.ReminderStatus status, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Tenant ve reminder type'a göre
     */
    List<Reminder> findByTenantIdAndType(Long tenantId, Reminder.ReminderType type);
    
    /**
     * Tenant, status ve retryCount'tan az olanlar
     */
    List<Reminder> findByTenantIdAndStatusAndRetryCountLessThan(Long tenantId, Reminder.ReminderStatus status, int retryCount);
    
    /**
     * Tenant, status ve sentAt'tan sonra gönderilmiş olanlar
     */
    List<Reminder> findByTenantIdAndStatusAndSentAtAfter(Long tenantId, Reminder.ReminderStatus status, LocalDateTime sentAtAfter);
}
