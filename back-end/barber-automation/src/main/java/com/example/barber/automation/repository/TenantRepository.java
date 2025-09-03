package com.example.barber.automation.repository;

import com.example.barber.automation.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Tenant (Kuaför) Repository
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    
    /**
     * Telefon numarasına göre kuaför bulma (WhatsApp entegrasyonu için)
     */
    Optional<Tenant> findByPhoneNumber(String phoneNumber);
    
    /**
     * Kuaför adına göre bulma
     */
    Optional<Tenant> findByName(String name);
    
    /**
     * Aktif kuaförleri listeleme
     */
    List<Tenant> findByActiveTrue();
    
    /**
     * Kuaför adına göre arama
     */
    @Query("SELECT t FROM Tenant t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%')) AND t.active = true")
    List<Tenant> findByNameContainingIgnoreCaseAndActiveTrue(@Param("name") String name);
    
    /**
     * Email adresine göre kuaför bulma
     */
    Optional<Tenant> findByEmailAndActiveTrue(String email);
    
    /**
     * Telefon numarasının başka bir kuaför tarafından kullanılıp kullanılmadığını kontrol etme
     */
    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);
    
    /**
     * Email adresinin başka bir kuaför tarafından kullanılıp kullanılmadığını kontrol etme
     */
    boolean existsByEmailAndIdNot(String email, Long id);
    
    /**
     * Aktif kuaför sayısını getirme
     */
    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.active = true")
    long countActiveTenants();
    
    /**
     * Aktif kuaför sayısı (Spring Data method naming ile)
     */
    long countByActiveTrue();
    
    /**
     * Telefon numarası varlığını kontrol etme
     */
    boolean existsByPhoneNumber(String phoneNumber);
    
    /**
     * Aktif kuaförleri oluşturulma tarihine göre sıralı getirme
     */
    List<Tenant> findByActiveTrueOrderByCreatedAtDesc();
    
    /**
     * Şehir ve ilçeye göre aktif kuaförleri bulma
     */
    @Query("SELECT t FROM Tenant t WHERE LOWER(t.city) = LOWER(:city) AND LOWER(t.district) = LOWER(:district) AND t.active = true")
    List<Tenant> findByCityAndDistrictAndActiveTrue(@Param("city") String city, @Param("district") String district);
    
    /**
     * Şehre göre aktif kuaförleri bulma
     */
    @Query("SELECT t FROM Tenant t WHERE LOWER(t.city) = LOWER(:city) AND t.active = true")
    List<Tenant> findByCityAndActiveTrue(@Param("city") String city);
}
