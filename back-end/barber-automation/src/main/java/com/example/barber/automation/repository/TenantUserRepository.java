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
}
