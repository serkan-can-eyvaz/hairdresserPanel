package com.example.barber.automation.repository;

import com.example.barber.automation.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service (Hizmet) Repository
 */
@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
    
    /**
     * Tenant'a ait aktif hizmetleri listeleme (sıralı)
     */
    List<Service> findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(Long tenantId);
    
    /**
     * Tenant'a ait tüm hizmetleri listeleme
     */
    List<Service> findByTenantIdOrderBySortOrderAscNameAsc(Long tenantId);
    
    /**
     * Tenant'a ait belirli bir hizmeti bulma
     */
    Optional<Service> findByIdAndTenantId(Long id, Long tenantId);
    
    /**
     * Tenant'a ait hizmet adına göre arama
     */
    @Query("SELECT s FROM Service s WHERE s.tenant.id = :tenantId AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')) AND s.active = true ORDER BY s.sortOrder ASC, s.name ASC")
    List<Service> findByTenantIdAndNameContainingIgnoreCaseAndActiveTrue(@Param("tenantId") Long tenantId, @Param("name") String name);
    
    /**
     * Tenant'a ait belirli süre aralığındaki hizmetleri bulma
     */
    @Query("SELECT s FROM Service s WHERE s.tenant.id = :tenantId AND s.durationMinutes BETWEEN :minDuration AND :maxDuration AND s.active = true ORDER BY s.durationMinutes ASC")
    List<Service> findByTenantIdAndDurationBetween(@Param("tenantId") Long tenantId, @Param("minDuration") Integer minDuration, @Param("maxDuration") Integer maxDuration);
    
    /**
     * Tenant'a ait aynı isimde hizmet olup olmadığını kontrol etme
     */
    boolean existsByTenantIdAndNameIgnoreCaseAndIdNot(Long tenantId, String name, Long id);
    
    /**
     * Tenant'a ait aktif hizmet sayısını getirme
     */
    long countByTenantIdAndActiveTrue(Long tenantId);
    
    /**
     * En popüler hizmetleri getirme (randevu sayısına göre)
     */
    @Query("SELECT s FROM Service s LEFT JOIN s.appointments a WHERE s.tenant.id = :tenantId AND s.active = true GROUP BY s ORDER BY COUNT(a) DESC")
    List<Service> findMostPopularServicesByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Aktif hizmetler sort order'a göre sıralı
     */
    List<Service> findByTenantIdAndActiveTrueOrderBySortOrder(Long tenantId);
    
    /**
     * İsme göre hizmet arama (alternatif method signature)
     */
    @Query("SELECT s FROM Service s WHERE s.tenant.id = :tenantId AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')) AND s.active = true ORDER BY s.sortOrder ASC, s.name ASC")
    List<Service> findByNameContainingIgnoreCaseAndTenantIdAndActiveTrue(@Param("name") String name, @Param("tenantId") Long tenantId);
    
    /**
     * Süre aralığına göre hizmet filtreleme
     */
    @Query("SELECT s FROM Service s WHERE s.tenant.id = :tenantId AND s.active = true AND s.durationMinutes BETWEEN :minDuration AND :maxDuration ORDER BY s.durationMinutes ASC")
    List<Service> findByTenantIdAndActiveTrueAndDurationMinutesBetween(@Param("tenantId") Long tenantId, @Param("minDuration") Integer minDuration, @Param("maxDuration") Integer maxDuration);
    
    /**
     * Fiyat aralığına göre hizmet filtreleme
     */
    @Query("SELECT s FROM Service s WHERE s.tenant.id = :tenantId AND s.active = true AND s.price BETWEEN :minPrice AND :maxPrice ORDER BY s.price ASC")
    List<Service> findByTenantIdAndActiveTrueAndPriceBetween(@Param("tenantId") Long tenantId, @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);
    
    /**
     * En yüksek sort order bulma
     */
    @Query("SELECT MAX(s.sortOrder) FROM Service s WHERE s.tenant.id = :tenantId AND s.active = true")
    Integer findMaxSortOrderByTenantIdAndActiveTrue(@Param("tenantId") Long tenantId);
    
    /**
     * Hizmet adı benzersizlik kontrolü (aktif olanlar)
     */
    boolean existsByNameAndTenantIdAndActiveTrue(String name, Long tenantId);
    
    /**
     * Hızlı hizmetler (belirtilen süreden kısa)
     */
    @Query("SELECT s FROM Service s WHERE s.tenant.id = :tenantId AND s.active = true AND s.durationMinutes < :maxDuration ORDER BY s.durationMinutes ASC")
    List<Service> findByTenantIdAndActiveTrueAndDurationMinutesLessThan(@Param("tenantId") Long tenantId, @Param("maxDuration") Integer maxDuration);
    
    /**
     * Ortalama fiyat hesaplama
     */
    @Query("SELECT AVG(s.price) FROM Service s WHERE s.tenant.id = :tenantId AND s.active = true")
    Double findAveragePriceByTenantIdAndActiveTrue(@Param("tenantId") Long tenantId);
    
    /**
     * Tenant ve isme göre hizmet varlığını kontrol etme
     */
    boolean existsByNameAndTenant(String name, com.example.barber.automation.entity.Tenant tenant);
}
