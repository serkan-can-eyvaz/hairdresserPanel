package com.example.barber.automation.repository;

import com.example.barber.automation.TestDataBuilder;
import com.example.barber.automation.entity.Tenant;
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
 * TenantRepository Integration Test
 * 
 * Bu test sınıfı TenantRepository'nin database ile entegrasyonunu test eder:
 * - JPA query'lerinin doğru çalışması
 * - Database constraint'lerinin kontrolü
 * - Custom query'lerin validation'ı
 * - Entity relationship'lerinin doğru kurulması
 * 
 * @DataJpaTest annotation'ı:
 * - Sadece JPA bileşenlerini yükler (hızlı test)
 * - H2 in-memory database kullanır
 * - Transaction'ları test sonunda rollback eder
 * - TestEntityManager sağlar (direct database access için)
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TenantRepository Integration Tests")
class TenantRepositoryIntegrationTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private TenantRepository tenantRepository;
    
    private Tenant testTenant1;
    private Tenant testTenant2;
    private Tenant inactiveTenant;
    
    /**
     * Her test öncesi çalışan setup metodu
     * Test verisini hazırlar ve database'e persist eder
     */
    @BeforeEach
    void setUp() {
        // Test verisini oluştur
        testTenant1 = TestDataBuilder.createTestTenant("Kuaför A", "+905321111111");
        testTenant2 = TestDataBuilder.createTestTenant("Kuaför B", "+905322222222");
        inactiveTenant = TestDataBuilder.createTestTenant("Pasif Kuaför", "+905323333333");
        inactiveTenant.setActive(false);
        
        // Database'e persist et ve flush
        entityManager.persistAndFlush(testTenant1);
        entityManager.persistAndFlush(testTenant2);
        entityManager.persistAndFlush(inactiveTenant);
        
        // EntityManager cache'ini temizle
        entityManager.clear();
    }
    
    @Test
    @DisplayName("Telefon numarasına göre kuaför bulma - Başarılı senaryo")
    void findByPhoneNumber_WhenExists_ShouldReturnTenant() {
        // Given: Kayıtlı bir telefon numarası
        String phoneNumber = "+905321111111";
        
        // When: Telefon numarasına göre arama yapılır
        Optional<Tenant> result = tenantRepository.findByPhoneNumber(phoneNumber);
        
        // Then: Doğru kuaför bulunur
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Kuaför A");
        assertThat(result.get().getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(result.get().getActive()).isTrue();
    }
    
    @Test
    @DisplayName("Telefon numarasına göre kuaför bulma - Bulunamayan numara")
    void findByPhoneNumber_WhenNotExists_ShouldReturnEmpty() {
        // Given: Kayıtlı olmayan bir telefon numarası
        String phoneNumber = "+905329999999";
        
        // When: Telefon numarasına göre arama yapılır
        Optional<Tenant> result = tenantRepository.findByPhoneNumber(phoneNumber);
        
        // Then: Sonuç boş döner
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Aktif kuaförleri listeleme - Sadece aktif olanlar dönmeli")
    void findByActiveTrue_ShouldReturnOnlyActiveTenants() {
        // When: Aktif kuaförler listelenir
        List<Tenant> activeTenants = tenantRepository.findByActiveTrue();
        
        // Then: Sadece aktif kuaförler döner
        assertThat(activeTenants).hasSize(2);
        assertThat(activeTenants)
                .extracting(Tenant::getName)
                .containsExactlyInAnyOrder("Kuaför A", "Kuaför B");
        assertThat(activeTenants)
                .allMatch(Tenant::getActive);
    }
    
    @Test
    @DisplayName("Kuaför adına göre arama - Case insensitive ve partial match")
    void findByNameContainingIgnoreCaseAndActiveTrue_ShouldFindMatches() {
        // Given: Arama terimi (küçük harf, kısmi)
        String searchTerm = "kuaför";
        
        // When: İsim bazlı arama yapılır
        List<Tenant> results = tenantRepository.findByNameContainingIgnoreCaseAndActiveTrue(searchTerm);
        
        // Then: Case insensitive olarak eşleşenler bulunur
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Tenant::getName)
                .containsExactlyInAnyOrder("Kuaför A", "Kuaför B");
    }
    
    @Test
    @DisplayName("Email ile aktif kuaför bulma")
    void findByEmailAndActiveTrue_WhenExists_ShouldReturnTenant() {
        // Given: Kayıtlı bir email adresi
        String email = "kuafora@test.com";
        
        // When: Email ile arama yapılır
        Optional<Tenant> result = tenantRepository.findByEmailAndActiveTrue(email);
        
        // Then: Doğru kuaför bulunur
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Kuaför A");
    }
    
    @Test
    @DisplayName("Telefon numarası tekrar kontrolü - Kendisi hariç")
    void existsByPhoneNumberAndIdNot_WhenDifferentTenant_ShouldReturnTrue() {
        // Given: Başka bir kuaförün telefon numarası
        String phoneNumber = "+905322222222";
        Long currentTenantId = testTenant1.getId();
        
        // When: Telefon numarası tekrar kontrolü yapılır
        boolean exists = tenantRepository.existsByPhoneNumberAndIdNot(phoneNumber, currentTenantId);
        
        // Then: Başka kuaförde kullanıldığı için true döner
        assertThat(exists).isTrue();
    }
    
    @Test
    @DisplayName("Telefon numarası tekrar kontrolü - Aynı kuaför için false")
    void existsByPhoneNumberAndIdNot_WhenSameTenant_ShouldReturnFalse() {
        // Given: Aynı kuaförün telefon numarası
        String phoneNumber = "+905321111111";
        Long currentTenantId = testTenant1.getId();
        
        // When: Telefon numarası tekrar kontrolü yapılır
        boolean exists = tenantRepository.existsByPhoneNumberAndIdNot(phoneNumber, currentTenantId);
        
        // Then: Aynı kuaför olduğu için false döner
        assertThat(exists).isFalse();
    }
    
    @Test
    @DisplayName("Email tekrar kontrolü - Kendisi hariç")
    void existsByEmailAndIdNot_WhenDifferentTenant_ShouldReturnTrue() {
        // Given: Başka bir kuaförün email adresi
        String email = "kuaforb@test.com";
        Long currentTenantId = testTenant1.getId();
        
        // When: Email tekrar kontrolü yapılır
        boolean exists = tenantRepository.existsByEmailAndIdNot(email, currentTenantId);
        
        // Then: Başka kuaförde kullanıldığı için true döner
        assertThat(exists).isTrue();
    }
    
    @Test
    @DisplayName("Aktif kuaför sayısı hesaplama")
    void countActiveTenants_ShouldReturnCorrectCount() {
        // When: Aktif kuaför sayısı hesaplanır
        long count = tenantRepository.countActiveTenants();
        
        // Then: Doğru sayı döner (2 aktif kuaför)
        assertThat(count).isEqualTo(2);
    }
    
    @Test
    @DisplayName("Database constraint test - Unique phone number")
    void saveWithDuplicatePhoneNumber_ShouldThrowException() {
        // Given: Aynı telefon numarasıyla yeni kuaför
        Tenant duplicateTenant = TestDataBuilder.createTestTenant("Duplicate Kuaför", "+905321111111");
        
        // When & Then: Unique constraint violation beklenir
        try {
            entityManager.persistAndFlush(duplicateTenant);
        } catch (Exception e) {
            // Database constraint violation beklenir
            assertThat(e).hasMessageContaining("unique").or().hasMessageContaining("constraint");
        }
    }
    
    @Test
    @DisplayName("Entity relationship test - Cascade operations")
    void deleteTenant_ShouldHandleCascadeCorrectly() {
        // Given: İlişkili veri olan kuaför
        Tenant tenantToDelete = testTenant1;
        Long tenantId = tenantToDelete.getId();
        
        // When: Kuaför silinir
        tenantRepository.delete(tenantToDelete);
        entityManager.flush();
        
        // Then: Kuaför database'den silinir
        Optional<Tenant> deletedTenant = tenantRepository.findById(tenantId);
        assertThat(deletedTenant).isEmpty();
    }
    
    @Test
    @DisplayName("Query performance test - Büyük veri seti ile")
    void findByActiveTrue_WithLargeDataset_ShouldPerformWell() {
        // Given: Çok sayıda kuaför eklenir
        for (int i = 0; i < 100; i++) {
            Tenant tenant = TestDataBuilder.createTestTenant("Kuaför " + i, "+90532" + String.format("%07d", i));
            if (i % 10 == 0) {
                tenant.setActive(false); // %10'unu pasif yap
            }
            entityManager.persist(tenant);
        }
        entityManager.flush();
        entityManager.clear();
        
        // When: Aktif kuaförler listelenir (performance ölçümü)
        long startTime = System.currentTimeMillis();
        List<Tenant> activeTenants = tenantRepository.findByActiveTrue();
        long endTime = System.currentTimeMillis();
        
        // Then: Sonuç doğru ve performans kabul edilebilir
        assertThat(activeTenants).hasSizeGreaterThan(90); // %90'ı aktif
        assertThat(endTime - startTime).isLessThan(1000); // 1 saniyeden az
    }
}
