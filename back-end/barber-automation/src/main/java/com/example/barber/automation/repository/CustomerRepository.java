package com.example.barber.automation.repository;

import com.example.barber.automation.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Customer (Müşteri) Repository
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    /**
     * Telefon numarasına göre müşteri bulma (WhatsApp entegrasyonu için)
     */
    Optional<Customer> findByPhoneNumberAndTenantId(String phoneNumber, Long tenantId);
    
    /**
     * Tenant'a ait aktif müşterileri listeleme
     */
    List<Customer> findByTenantIdAndActiveTrueOrderByNameAsc(Long tenantId);
    
    /**
     * Tenant'a ait aktif müşterileri listeleme (basit)
     */
    List<Customer> findByTenantIdAndActiveTrue(Long tenantId);
    
    /**
     * Tenant'a ait belirli bir müşteriyi bulma
     */
    Optional<Customer> findByIdAndTenantId(Long id, Long tenantId);
    
    /**
     * Tenant'a ait müşteri adına göre arama
     */
    @Query("SELECT c FROM Customer c WHERE c.tenant.id = :tenantId AND LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) AND c.active = true ORDER BY c.name ASC")
    List<Customer> findByTenantIdAndNameContainingIgnoreCaseAndActiveTrue(@Param("tenantId") Long tenantId, @Param("name") String name);
    
    /**
     * İsme göre müşteri arama (alternatif method signature)
     */
    @Query("SELECT c FROM Customer c WHERE c.tenant.id = :tenantId AND LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')) AND c.active = true ORDER BY c.name ASC")
    List<Customer> findByNameContainingIgnoreCaseAndTenantIdAndActiveTrue(@Param("name") String name, @Param("tenantId") Long tenantId);
    
    /**
     * Belirli tarihten sonra randevusu olan müşteriler
     */
    @Query("SELECT DISTINCT c FROM Customer c JOIN c.appointments a WHERE c.tenant.id = :tenantId AND a.startTime >= :fromDate AND c.active = true ORDER BY c.name ASC")
    List<Customer> findByTenantIdAndHasAppointmentAfter(@Param("tenantId") Long tenantId, @Param("fromDate") LocalDateTime fromDate);
    
    /**
     * Son randevusu belirli tarihten önce olan müşteriler (hatırlatma için)
     */
    @Query("SELECT c FROM Customer c WHERE c.tenant.id = :tenantId AND c.active = true AND c.allowNotifications = true AND NOT EXISTS (SELECT 1 FROM Appointment a WHERE a.customer = c AND a.startTime >= :afterDate) AND EXISTS (SELECT 1 FROM Appointment a WHERE a.customer = c AND a.startTime <= :beforeDate AND a.status = 'COMPLETED')")
    List<Customer> findCustomersForReminder(@Param("tenantId") Long tenantId, @Param("beforeDate") LocalDateTime beforeDate, @Param("afterDate") LocalDateTime afterDate);
    
    /**
     * Tenant'a ait aynı telefon numarasında müşteri olup olmadığını kontrol etme
     */
    boolean existsByTenantIdAndPhoneNumberAndIdNot(Long tenantId, String phoneNumber, Long id);
    
    /**
     * Tenant'a ait aktif müşteri sayısını getirme
     */
    long countByTenantIdAndActiveTrue(Long tenantId);
    
    /**
     * En sadık müşterileri getirme (randevu sayısına göre)
     */
    @Query("SELECT c FROM Customer c LEFT JOIN c.appointments a WHERE c.tenant.id = :tenantId AND c.active = true AND a.status = 'COMPLETED' GROUP BY c ORDER BY COUNT(a) DESC")
    List<Customer> findMostLoyalCustomersByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Yeni müşterileri getirme (son 30 gün)
     */
    @Query("SELECT c FROM Customer c WHERE c.tenant.id = :tenantId AND c.active = true AND c.createdAt >= :since ORDER BY c.createdAt DESC")
    List<Customer> findNewCustomersSince(@Param("tenantId") Long tenantId, @Param("since") LocalDateTime since);
    
    /**
     * Email ile müşteri bulma
     */
    Optional<Customer> findByEmailAndTenantId(String email, Long tenantId);
    
    /**
     * Bildirimlere izin veren aktif müşteriler
     */
    List<Customer> findByTenantIdAndAllowNotificationsTrueAndActiveTrue(Long tenantId);
    
    /**
     * Telefon numarası benzersizlik kontrolü (ID hariç)
     */
    boolean existsByPhoneNumberAndTenantIdAndIdNot(String phoneNumber, Long tenantId, Long id);
    
    /**
     * Oluşturma tarihine göre sıralı aktif müşteriler (en yeni en başta)
     */
    List<Customer> findByTenantIdAndActiveTrueOrderByCreatedAtDesc(Long tenantId);
    
    /**
     * Tenant'a ait aktif müşteri sayısı
     */
    long countByTenantIdAndActiveTrue(Long tenantId);
}
