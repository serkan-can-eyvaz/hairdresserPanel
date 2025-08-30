package com.example.barber.automation.repository;

import com.example.barber.automation.TestDataBuilder;
import com.example.barber.automation.entity.Tenant;
import com.example.barber.automation.entity.TenantUser;
import com.example.barber.automation.entity.TenantUser.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantUserRepository Integration Tests
 * JPA query doğrulaması ve veritabanı işlemleri testleri
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TenantUserRepository Integration Tests")
class TenantUserRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TenantUserRepository tenantUserRepository;

    private Tenant tenant1, tenant2;
    private TenantUser adminUser, employeeUser1, employeeUser2, inactiveUser;

    @BeforeEach
    void setUp() {
        // Test data oluştur
        tenant1 = TestDataBuilder.createTestTenant("Kuaför A", "+905321112233");
        tenant2 = TestDataBuilder.createTestTenant("Kuaför B", "+905376667788");
        
        entityManager.persistAndFlush(tenant1);
        entityManager.persistAndFlush(tenant2);
        
        // Test kullanıcıları oluştur
        adminUser = createTestUser(tenant1, "admin1", "admin@kuafora.com", "Ahmet", "Yılmaz", UserRole.TENANT_ADMIN, true);
        employeeUser1 = createTestUser(tenant1, "employee1", "emp1@kuafora.com", "Mehmet", "Demir", UserRole.EMPLOYEE, true);
        employeeUser2 = createTestUser(tenant1, "employee2", "emp2@kuafora.com", "Ayşe", "Kaya", UserRole.EMPLOYEE, true);
        inactiveUser = createTestUser(tenant1, "inactive1", "inactive@kuafora.com", "Ali", "Veli", UserRole.EMPLOYEE, false);
        
        // Tenant2 için kullanıcı
        createTestUser(tenant2, "admin2", "admin@kuaforb.com", "Fatma", "Özkan", UserRole.TENANT_ADMIN, true);
        
        entityManager.flush();
        entityManager.clear();
    }

    private TenantUser createTestUser(Tenant tenant, String username, String email, String firstName, 
                                     String lastName, UserRole role, Boolean active) {
        TenantUser user = new TenantUser();
        user.setTenant(tenant);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setActive(active);
        user.setPassword("hashedPassword");
        
        return entityManager.persistAndFlush(user);
    }

    @Test
    @DisplayName("Username ve tenant ID'ye göre kullanıcı bulma")
    void findByUsernameAndTenantId_WhenExists_ShouldReturnUser() {
        // When
        Optional<TenantUser> result = tenantUserRepository.findByUsernameAndTenantId("admin1", tenant1.getId());
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("admin1");
        assertThat(result.get().getRole()).isEqualTo(UserRole.TENANT_ADMIN);
        assertThat(result.get().getTenant().getId()).isEqualTo(tenant1.getId());
    }

    @Test
    @DisplayName("Var olmayan username için empty döndürme")
    void findByUsernameAndTenantId_WhenNotExists_ShouldReturnEmpty() {
        // When
        Optional<TenantUser> result = tenantUserRepository.findByUsernameAndTenantId("nonexistent", tenant1.getId());
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Email ve tenant ID'ye göre kullanıcı bulma")
    void findByEmailAndTenantId_WhenExists_ShouldReturnUser() {
        // When
        Optional<TenantUser> result = tenantUserRepository.findByEmailAndTenantId("admin@kuafora.com", tenant1.getId());
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("admin@kuafora.com");
        assertThat(result.get().getRole()).isEqualTo(UserRole.TENANT_ADMIN);
        assertThat(result.get().getTenant().getId()).isEqualTo(tenant1.getId());
    }

    @Test
    @DisplayName("Username ile aktif kullanıcı bulma")
    void findByUsernameAndActiveTrue_WhenActive_ShouldReturnUser() {
        // When
        Optional<TenantUser> result = tenantUserRepository.findByUsernameAndActiveTrue("admin1");
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("admin1");
        assertThat(result.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("İnaktif kullanıcı için empty döndürme")
    void findByUsernameAndActiveTrue_WhenInactive_ShouldReturnEmpty() {
        // When
        Optional<TenantUser> result = tenantUserRepository.findByUsernameAndActiveTrue("inactive1");
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Tenant'a ait aktif kullanıcıları listeleme")
    void findByTenantIdAndActiveTrue_ShouldReturnActiveUsers() {
        // When
        List<TenantUser> result = tenantUserRepository.findByTenantIdAndActiveTrue(tenant1.getId());
        
        // Then
        assertThat(result).hasSize(3); // admin, emp1, emp2 (inactive hariç)
        assertThat(result.stream())
            .allMatch(user -> user.getTenant().getId().equals(tenant1.getId()) && user.isActive());
    }

    @Test
    @DisplayName("Ad veya soyad ile arama yapma (case insensitive)")
    void findByTenantIdAndFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase_ShouldReturnMatchingUsers() {
        // When
        List<TenantUser> result = tenantUserRepository
            .findByTenantIdAndFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                tenant1.getId(), "MEHMET", "KAYA");
        
        // Then
        assertThat(result).hasSize(2); // Mehmet Demir, Ayşe Kaya
        assertThat(result.stream().map(TenantUser::getFirstName))
            .containsExactlyInAnyOrder("Mehmet", "Ayşe");
    }

    @Test
    @DisplayName("Username benzersizlik kontrolü - aynı tenant içinde")
    void existsByUsernameAndTenantIdAndIdNot_ShouldReturnCorrectValue() {
        // When & Then
        // Aynı tenant içinde başka bir kullanıcının username'ini kontrol et
        assertThat(tenantUserRepository.existsByUsernameAndTenantIdAndIdNot(
            "employee1", tenant1.getId(), adminUser.getId())).isTrue();
        
        // Kendi username'ini kontrol et (kendisi hariç)
        assertThat(tenantUserRepository.existsByUsernameAndTenantIdAndIdNot(
            "admin1", tenant1.getId(), adminUser.getId())).isFalse();
    }

    @Test
    @DisplayName("Email benzersizlik kontrolü - aynı tenant içinde")
    void existsByEmailAndTenantIdAndIdNot_ShouldReturnCorrectValue() {
        // When & Then
        // Aynı tenant içinde başka bir kullanıcının email'ini kontrol et
        assertThat(tenantUserRepository.existsByEmailAndTenantIdAndIdNot(
            "emp1@kuafora.com", tenant1.getId(), adminUser.getId())).isTrue();
        
        // Kendi email'ini kontrol et (kendisi hariç)
        assertThat(tenantUserRepository.existsByEmailAndTenantIdAndIdNot(
            "admin@kuafora.com", tenant1.getId(), adminUser.getId())).isFalse();
    }

    @Test
    @DisplayName("Tenant'a ait kullanıcıları en yeni önce sıralı getirme")
    void findByTenantIdAndActiveTrueOrderByCreatedAtDesc_ShouldReturnUsersInDescOrder() {
        // When
        List<TenantUser> result = tenantUserRepository.findByTenantIdAndActiveTrueOrderByCreatedAtDesc(tenant1.getId());
        
        // Then
        assertThat(result).hasSize(3);
        // En yeni kullanıcı ilk sırada olmalı
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).getCreatedAt())
                .isAfterOrEqualTo(result.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("Role'e göre filtreleme")
    void findByTenantIdAndRoleAndActiveTrue_ShouldReturnUsersByRole() {
        // When
        List<TenantUser> adminUsers = tenantUserRepository.findByTenantIdAndRoleAndActiveTrue(
            tenant1.getId(), UserRole.TENANT_ADMIN);
        List<TenantUser> employeeUsers = tenantUserRepository.findByTenantIdAndRoleAndActiveTrue(
            tenant1.getId(), UserRole.EMPLOYEE);
        
        // Then
        assertThat(adminUsers).hasSize(1);
        assertThat(adminUsers.get(0).getRole()).isEqualTo(UserRole.TENANT_ADMIN);
        
        assertThat(employeeUsers).hasSize(2); // employee1, employee2 (inactive hariç)
        assertThat(employeeUsers.stream())
            .allMatch(user -> user.getRole() == UserRole.EMPLOYEE && user.isActive());
    }

    @Test
    @DisplayName("Admin kullanıcıları bulma")
    void findAdminUsersByTenantId_ShouldReturnAdminUsers() {
        // When
        List<TenantUser> result = tenantUserRepository.findAdminUsersByTenantId(tenant1.getId());
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(UserRole.TENANT_ADMIN);
        assertThat(result.get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("Tenant'a ait aktif kullanıcı sayısı")
    void countByTenantIdAndActiveTrue_ShouldReturnCorrectCount() {
        // When
        long count = tenantUserRepository.countByTenantIdAndActiveTrue(tenant1.getId());
        
        // Then
        assertThat(count).isEqualTo(3); // admin, emp1, emp2 (inactive hariç)
    }

    @Test
    @DisplayName("Data isolation - Tenant'lar arası veri izolasyonu")
    void testDataIsolation_ShouldNotReturnOtherTenantUsers() {
        // When
        List<TenantUser> tenant1Users = tenantUserRepository.findByTenantIdAndActiveTrue(tenant1.getId());
        List<TenantUser> tenant2Users = tenantUserRepository.findByTenantIdAndActiveTrue(tenant2.getId());
        
        long tenant1Count = tenantUserRepository.countByTenantIdAndActiveTrue(tenant1.getId());
        long tenant2Count = tenantUserRepository.countByTenantIdAndActiveTrue(tenant2.getId());
        
        // Then
        assertThat(tenant1Users).hasSize(3);
        assertThat(tenant2Users).hasSize(1);
        
        assertThat(tenant1Count).isEqualTo(3);
        assertThat(tenant2Count).isEqualTo(1);
        
        // Verify cross-tenant isolation
        assertThat(tenant1Users.stream())
            .allMatch(user -> user.getTenant().getId().equals(tenant1.getId()));
        assertThat(tenant2Users.stream())
            .allMatch(user -> user.getTenant().getId().equals(tenant2.getId()));
    }

    @Test
    @DisplayName("Kullanıcı aktiflik durumu kontrolü")
    void isActive_ShouldReturnCorrectStatus() {
        // Given
        Optional<TenantUser> activeUser = tenantUserRepository.findByUsernameAndTenantId("admin1", tenant1.getId());
        Optional<TenantUser> inactiveUserOptional = tenantUserRepository.findByUsernameAndTenantId("inactive1", tenant1.getId());
        
        // When & Then
        assertThat(activeUser).isPresent();
        assertThat(activeUser.get().isActive()).isTrue();
        
        assertThat(inactiveUserOptional).isPresent();
        assertThat(inactiveUserOptional.get().isActive()).isFalse();
    }

    @Test
    @DisplayName("Kullanıcı tam adı getirme")
    void getFullName_ShouldReturnCorrectFullName() {
        // Given
        Optional<TenantUser> user = tenantUserRepository.findByUsernameAndTenantId("admin1", tenant1.getId());
        
        // When & Then
        assertThat(user).isPresent();
        assertThat(user.get().getFullName()).isEqualTo("Ahmet Yılmaz");
    }

    @Test
    @DisplayName("İnaktif kullanıcı aktivasyonu test")
    void activateInactiveUser_ShouldWork() {
        // Given
        Optional<TenantUser> inactiveUserOptional = tenantUserRepository.findByUsernameAndTenantId("inactive1", tenant1.getId());
        assertThat(inactiveUserOptional).isPresent();
        
        TenantUser inactiveUser = inactiveUserOptional.get();
        assertThat(inactiveUser.isActive()).isFalse();
        
        // When
        inactiveUser.setActive(true);
        entityManager.merge(inactiveUser);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        Optional<TenantUser> activatedUser = tenantUserRepository.findByUsernameAndTenantId("inactive1", tenant1.getId());
        assertThat(activatedUser).isPresent();
        assertThat(activatedUser.get().isActive()).isTrue();
        
        // Aktif kullanıcı sayısı artmış olmalı
        long activeCount = tenantUserRepository.countByTenantIdAndActiveTrue(tenant1.getId());
        assertThat(activeCount).isEqualTo(4);
    }
}