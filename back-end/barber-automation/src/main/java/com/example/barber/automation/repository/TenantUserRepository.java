package com.example.barber.automation.repository;

import com.example.barber.automation.entity.TenantUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TenantUser (Kuaför Kullanıcıları) Repository
 */
@Repository
public interface TenantUserRepository extends JpaRepository<TenantUser, Long> {
    
    /**
     * Kullanıcı adına göre kullanıcı bulma (login için)
     */
    Optional<TenantUser> findByUsernameAndActiveTrue(String username);
    
    /**
     * Kullanıcı adına göre aktif kullanıcı bulma (overloaded method)
     */
    Optional<TenantUser> findByUsernameAndActive(String username, Boolean active);
    
    /**
     * Email adresine göre kullanıcı bulma
     */
    Optional<TenantUser> findByEmailAndActiveTrue(String email);
    
    /**
     * Tenant'a ait kullanıcıları listeleme
     */
    List<TenantUser> findByTenantIdAndActiveTrue(Long tenantId);
    
    /**
     * Tenant'a ait belirli role sahip kullanıcıları listeleme
     */
    List<TenantUser> findByTenantIdAndRoleAndActiveTrue(Long tenantId, TenantUser.UserRole role);
    
    /**
     * Kullanıcı adının başka biri tarafından kullanılıp kullanılmadığını kontrol etme
     */
    boolean existsByUsernameAndIdNot(String username, Long id);
    
    /**
     * Email adresinin başka biri tarafından kullanılıp kullanılmadığını kontrol etme
     */
    boolean existsByEmailAndIdNot(String email, Long id);
    
    /**
     * Tenant'ın admin kullanıcılarını bulma
     */
    @Query("SELECT u FROM TenantUser u WHERE u.tenant.id = :tenantId AND u.role IN ('TENANT_ADMIN', 'SUPER_ADMIN') AND u.active = true")
    List<TenantUser> findAdminUsersByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Tenant'a ait kullanıcı sayısını getirme
     */
    long countByTenantIdAndActiveTrue(Long tenantId);
    
    // Test'ler için eksik method'lar
    
    /**
     * Username ve tenant id'ye göre kullanıcı bulma
     */
    Optional<TenantUser> findByUsernameAndTenantId(String username, Long tenantId);
    
    /**
     * Email ve tenant id'ye göre kullanıcı bulma 
     */
    Optional<TenantUser> findByEmailAndTenantId(String email, Long tenantId);
    
    /**
     * Tenant id'ye göre ad veya soyad arama (büyük/küçük harf duyarsız)
     */
    @Query("SELECT u FROM TenantUser u WHERE u.tenant.id = :tenantId AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))")
    List<TenantUser> findByTenantIdAndFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("firstName") String firstName, @Param("lastName") String lastName);
    
    /**
     * Username'in aynı tenant içinde başka bir user tarafından kullanılıp kullanılmadığı
     */
    boolean existsByUsernameAndTenantIdAndIdNot(String username, Long tenantId, Long id);
    
    /**
     * Email'in aynı tenant içinde başka bir user tarafından kullanılıp kullanılmadığı
     */
    boolean existsByEmailAndTenantIdAndIdNot(String email, Long tenantId, Long id);
    
    /**
     * Tenant'a ait aktif kullanıcıları en yeni önce sıralı getirme
     */
    List<TenantUser> findByTenantIdAndActiveTrueOrderByCreatedAtDesc(Long tenantId);
    
    /**
     * Username varlığını kontrol etme
     */
    boolean existsByUsername(String username);
    
    /**
     * Email varlığını kontrol etme
     */
    boolean existsByEmail(String email);
    
    /**
     * Role göre kullanıcı varlığını kontrol etme (Super admin kontrolü için)
     */
    boolean existsByRole(TenantUser.UserRole role);
}
