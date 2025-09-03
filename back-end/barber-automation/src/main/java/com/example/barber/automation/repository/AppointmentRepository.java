package com.example.barber.automation.repository;

import com.example.barber.automation.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Appointment (Randevu) Repository
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    
    /**
     * Tenant'a ait belirli bir randevuyu bulma
     */
    Optional<Appointment> findByIdAndTenantId(Long id, Long tenantId);
    
    /**
     * Tenant'a ait aktif randevuları listeleme (tarih sıralı)
     */
    @Query("SELECT a FROM Appointment a WHERE a.tenant.id = :tenantId AND a.status IN ('PENDING', 'CONFIRMED') ORDER BY a.startTime ASC")
    List<Appointment> findActiveAppointmentsByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Belirli tarih aralığındaki randevuları getirme
     */
    @Query("SELECT a FROM Appointment a WHERE a.tenant.id = :tenantId AND a.startTime BETWEEN :startDate AND :endDate ORDER BY a.startTime ASC")
    List<Appointment> findByTenantIdAndDateRange(@Param("tenantId") Long tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Müşteriye ait randevuları getirme
     */
    @Query("SELECT a FROM Appointment a WHERE a.tenant.id = :tenantId AND a.customer.id = :customerId ORDER BY a.startTime DESC")
    List<Appointment> findByTenantIdAndCustomerId(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);
    
    /**
     * Belirli zaman aralığında çakışan randevuları kontrol etme
     */
    @Query("SELECT a FROM Appointment a WHERE a.tenant.id = :tenantId AND a.status IN ('PENDING', 'CONFIRMED') AND ((a.startTime < :endTime AND a.endTime > :startTime))")
    List<Appointment> findConflictingAppointments(@Param("tenantId") Long tenantId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * Günlük randevuları getirme
     */
    @Query("SELECT a FROM Appointment a WHERE a.tenant.id = :tenantId AND DATE(a.startTime) = DATE(:date) ORDER BY a.startTime ASC")
    List<Appointment> findDailyAppointments(@Param("tenantId") Long tenantId, @Param("date") LocalDateTime date);
    
    /**
     * Bugünkü randevuları getirme
     */
    @Query("SELECT a FROM Appointment a WHERE a.tenant.id = :tenantId AND CAST(a.startTime AS DATE) = CURRENT_DATE() ORDER BY a.startTime ASC")
    List<Appointment> findTodayAppointments(@Param("tenantId") Long tenantId);
    
    /**
     * Yaklaşan randevuları getirme (gelecek 7 gün)
     */
    @Query("SELECT a FROM Appointment a WHERE a.tenant.id = :tenantId AND a.status IN ('PENDING', 'CONFIRMED') AND a.startTime BETWEEN :now AND :weekFromNow ORDER BY a.startTime ASC")
    List<Appointment> findUpcomingAppointments(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now, @Param("weekFromNow") LocalDateTime weekFromNow);
    
    /**
     * Belirli müşterinin aktif randevusu olup olmadığını kontrol etme
     */
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.tenant.id = :tenantId AND a.customer.id = :customerId AND a.status IN ('PENDING', 'CONFIRMED')")
    boolean hasActiveAppointment(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);
    
    /**
     * Müşterinin son tamamlanan randevusunu getirme
     */
    @Query("SELECT a FROM Appointment a WHERE a.tenant.id = :tenantId AND a.customer.id = :customerId AND a.status = 'COMPLETED' ORDER BY a.startTime DESC LIMIT 1")
    Optional<Appointment> findLastCompletedAppointment(@Param("tenantId") Long tenantId, @Param("customerId") Long customerId);
    
    /**
     * Belirli statüdeki randevu sayısını getirme
     */
    long countByTenantIdAndStatus(Long tenantId, Appointment.AppointmentStatus status);
    
    /**
     * Aylık randevu istatistikleri
     */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.tenant.id = :tenantId AND YEAR(a.startTime) = :year AND MONTH(a.startTime) = :month AND a.status = 'COMPLETED'")
    long countMonthlyCompletedAppointments(@Param("tenantId") Long tenantId, @Param("year") int year, @Param("month") int month);
    
    /**
     * Hatırlatma gönderilmemiş tamamlanan randevuları getirme
     */
    @Query("SELECT a FROM Appointment a WHERE a.tenant.id = :tenantId AND a.status = 'COMPLETED' AND a.reminderSent = false AND a.endTime <= :beforeDate")
    List<Appointment> findCompletedAppointmentsWithoutReminder(@Param("tenantId") Long tenantId, @Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * Belirli tarih aralığında belirli statüdeki randevuları getirme
     */
    List<Appointment> findByStartTimeBetweenAndStatus(LocalDateTime startTime, LocalDateTime endTime, Appointment.AppointmentStatus status);
    
    /**
     * Belirli tarih aralığında belirli statüdeki randevuları sayma
     */
    long countByStartTimeBetweenAndStatus(LocalDateTime startTime, LocalDateTime endTime, Appointment.AppointmentStatus status);
    
    /**
     * Hatırlatma gönderilmemiş belirli statüdeki randevuları getirme
     */
    List<Appointment> findByStartTimeBetweenAndStatusAndReminderSentFalse(LocalDateTime startTime, LocalDateTime endTime, Appointment.AppointmentStatus status);
    
    /**
     * Kaçırılan randevuları bulma (belirli saatten önce başlayan ama hala pending/confirmed olan)
     */
    @Query("SELECT a FROM Appointment a WHERE a.startTime < :cutoffTime AND a.status IN ('PENDING', 'CONFIRMED')")
    List<Appointment> findMissedAppointments(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Belirli tarih aralığındaki randevu sayısı
     */
    long countByStartTimeBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    /**
     * Tenant ve tarih aralığına göre randevu sayısı
     */
    long countByTenantIdAndStartTimeBetween(Long tenantId, LocalDateTime startDateTime, LocalDateTime endDateTime);
    
    /**
     * Tenant'a ait toplam randevu sayısı
     */
    long countByTenantId(Long tenantId);
    
    /**
     * Tenant'a ait tüm randevuları tarih sıralı getirme
     */
    List<Appointment> findByTenantIdOrderByStartTimeDesc(Long tenantId);
    
    /**
     * Tüm randevuları tarih sıralı getirme
     */
    List<Appointment> findAllByOrderByStartTimeDesc();
    
    /**
     * Belirli tenant ve tarih aralığında belirli statüde olmayan randevuları getirme
     */
    List<Appointment> findByTenantIdAndStartTimeBetweenAndStatusNot(Long tenantId, LocalDateTime startTime, LocalDateTime endTime, String status);
}
