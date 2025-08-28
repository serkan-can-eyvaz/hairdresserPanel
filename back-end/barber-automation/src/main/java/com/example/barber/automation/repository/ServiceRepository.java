package com.example.barber.automation.repository;

import com.example.barber.automation.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
