package com.example.barber.automation.repository;

import com.example.barber.automation.TestDataBuilder;
import com.example.barber.automation.entity.Customer;
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
 * Customer Repository Integration Tests
 * 
 * CustomerRepository'nin JPA query'lerini ve entity ilişkilerini test eder.
 * 
 * Test kapsamı:
 * - Temel CRUD operasyonları
 * - Custom query'ler
 * - Multi-tenant izolasyon
 * - Entity ilişkileri
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CustomerRepository Integration Tests")
class CustomerRepositoryIntegrationTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    private Tenant testTenant1;
    private Tenant testTenant2;
    private Customer testCustomer1;
    private Customer testCustomer2;
    private Customer inactiveCustomer;
    
    /**
     * Her test öncesi çalışan setup metodu
     * Test verisini hazırlar ve database'e persist eder
     */
    @BeforeEach
    void setUp() {
        // Test tenant'ları oluştur
        testTenant1 = TestDataBuilder.createTestTenant("Kuaför A", "+905321111111");
        testTenant2 = TestDataBuilder.createTestTenant("Kuaför B", "+905322222222");
        
        testTenant1 = entityManager.persistAndFlush(testTenant1);
        testTenant2 = entityManager.persistAndFlush(testTenant2);
        
        // Test müşterilerini oluştur
        testCustomer1 = TestDataBuilder.createTestCustomer("Ali Veli", "+905311234567", testTenant1);
        testCustomer2 = TestDataBuilder.createTestCustomer("Ayşe Yılmaz", "+905321234567", testTenant1);
        inactiveCustomer = TestDataBuilder.createTestCustomer("Pasif Müşteri", "+905331234567", testTenant1);
        inactiveCustomer.setActive(false);
        
        // Customer'ları persist et
        testCustomer1 = entityManager.persistAndFlush(testCustomer1);
        testCustomer2 = entityManager.persistAndFlush(testCustomer2);
        inactiveCustomer = entityManager.persistAndFlush(inactiveCustomer);
        
        // Clear cache for fresh queries
        entityManager.clear();
    }
    
    @Test
    @DisplayName("Telefon numarasına göre müşteri bulma - Başarılı")
    void findByPhoneNumberAndTenantId_WhenExists_ShouldReturnCustomer() {
        // When: Telefon numarası ile arama yapılır
        Optional<Customer> result = customerRepository.findByPhoneNumberAndTenantId(
                "+905311234567", testTenant1.getId());
        
        // Then: Doğru müşteri bulunur
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Ali Veli");
        assertThat(result.get().getTenant().getId()).isEqualTo(testTenant1.getId());
    }
    
    @Test
    @DisplayName("Telefon numarasına göre müşteri bulma - Bulunamadığında")
    void findByPhoneNumberAndTenantId_WhenNotExists_ShouldReturnEmpty() {
        // When: Olmayan telefon numarası ile arama yapılır
        Optional<Customer> result = customerRepository.findByPhoneNumberAndTenantId(
                "+905559999999", testTenant1.getId());
        
        // Then: Boş sonuç döner
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Aktif müşteriler tenant'a göre listeleme")
    void findByTenantIdAndActiveTrue_ShouldReturnActiveCustomersOnly() {
        // When: Aktif müşteriler listelenir
        List<Customer> activeCustomers = customerRepository.findByTenantIdAndActiveTrue(testTenant1.getId());
        
        // Then: Sadece aktif müşteriler dönmelidir
        assertThat(activeCustomers).hasSize(2);
        assertThat(activeCustomers)
                .extracting(Customer::getName)
                .containsExactlyInAnyOrder("Ali Veli", "Ayşe Yılmaz");
        
        // Pasif müşteri dahil edilmemeli
        assertThat(activeCustomers)
                .extracting(Customer::getName)
                .doesNotContain("Pasif Müşteri");
    }
    
    @Test
    @DisplayName("İsim ile arama - Büyük/küçük harf duyarsız")
    void findByNameContainingIgnoreCaseAndTenantIdAndActiveTrue_ShouldFindMatches() {
        // When: İsim ile arama yapılır (küçük harfle)
        List<Customer> results = customerRepository.findByNameContainingIgnoreCaseAndTenantIdAndActiveTrue(
                "ali", testTenant1.getId());
        
        // Then: Büyük/küçük harf fark etmeden bulur
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Ali Veli");
        
        // Partial match test
        List<Customer> partialResults = customerRepository.findByNameContainingIgnoreCaseAndTenantIdAndActiveTrue(
                "veli", testTenant1.getId());
        assertThat(partialResults).hasSize(1);
        assertThat(partialResults.get(0).getName()).isEqualTo("Ali Veli");
    }
    
    @Test
    @DisplayName("Tenant bazlı müşteri sayısı - Aktif olanlar")
    void countByTenantIdAndActiveTrue_ShouldReturnCorrectCount() {
        // When: Aktif müşteri sayısı hesaplanır
        long count = customerRepository.countByTenantIdAndActiveTrue(testTenant1.getId());
        
        // Then: Doğru sayı döner (pasif dahil değil)
        assertThat(count).isEqualTo(2);
        
        // Farklı tenant için 0 olmalı
        long otherTenantCount = customerRepository.countByTenantIdAndActiveTrue(testTenant2.getId());
        assertThat(otherTenantCount).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Email ile müşteri bulma")
    void findByEmailAndTenantId_WhenExists_ShouldReturnCustomer() {
        // Given: Müşterinin email adresi
        String email = testCustomer1.getEmail();
        
        // When: Email ile arama yapılır
        Optional<Customer> result = customerRepository.findByEmailAndTenantId(email, testTenant1.getId());
        
        // Then: Doğru müşteri bulunur
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Ali Veli");
    }
    
    @Test
    @DisplayName("Bildirimlere izin veren müşteriler")
    void findByTenantIdAndAllowNotificationsTrueAndActiveTrue_ShouldReturnNotificationAllowedCustomers() {
        // Given: Bildirimlere izin veren müşteri
        testCustomer1.setAllowNotifications(true);
        testCustomer2.setAllowNotifications(false);
        entityManager.merge(testCustomer1);
        entityManager.merge(testCustomer2);
        entityManager.flush();
        entityManager.clear();
        
        // When: Bildirimlere izin veren müşteriler listelenir
        List<Customer> notificationCustomers = customerRepository
                .findByTenantIdAndAllowNotificationsTrueAndActiveTrue(testTenant1.getId());
        
        // Then: Sadece izin veren müşteriler döner
        assertThat(notificationCustomers).hasSize(1);
        assertThat(notificationCustomers.get(0).getName()).isEqualTo("Ali Veli");
    }
    
    @Test
    @DisplayName("Multi-tenant izolasyon - Farklı tenant'ların müşterilerini görmemeli")
    void findByTenantIdAndActiveTrue_ShouldNotCrossTenantBoundaries() {
        // Given: Farklı tenant'a ait müşteri
        Customer otherTenantCustomer = TestDataBuilder.createTestCustomer("Başka Müşteri", "+905339999999", testTenant2);
        entityManager.persistAndFlush(otherTenantCustomer);
        entityManager.clear();
        
        // When: Tenant1'in müşterileri listelenir
        List<Customer> tenant1Customers = customerRepository.findByTenantIdAndActiveTrue(testTenant1.getId());
        
        // Then: Sadece kendi müşterilerini görür
        assertThat(tenant1Customers).hasSize(2);
        assertThat(tenant1Customers)
                .extracting(Customer::getName)
                .containsExactlyInAnyOrder("Ali Veli", "Ayşe Yılmaz")
                .doesNotContain("Başka Müşteri");
        
        // When: Tenant2'nin müşterileri listelenir
        List<Customer> tenant2Customers = customerRepository.findByTenantIdAndActiveTrue(testTenant2.getId());
        
        // Then: Sadece kendi müşterisini görür
        assertThat(tenant2Customers).hasSize(1);
        assertThat(tenant2Customers.get(0).getName()).isEqualTo("Başka Müşteri");
    }
    
    @Test
    @DisplayName("Telefon numarası benzersizlik kontrolü - Aynı tenant içinde")
    void existsByPhoneNumberAndTenantIdAndIdNot_ShouldCheckUniqueness() {
        // When: Aynı tenant'ta farklı customer'ın telefon numarasını kontrol et
        boolean exists = customerRepository.existsByPhoneNumberAndTenantIdAndIdNot(
                "+905321234567", testTenant1.getId(), testCustomer1.getId());
        
        // Then: Başka customer'da bu numara var, true döner
        assertThat(exists).isTrue();
        
        // When: Kendi telefon numarasını kontrol et
        boolean selfCheck = customerRepository.existsByPhoneNumberAndTenantIdAndIdNot(
                "+905311234567", testTenant1.getId(), testCustomer1.getId());
        
        // Then: Kendisi hariç başka customer'da yok, false döner
        assertThat(selfCheck).isFalse();
    }
    
    @Test
    @DisplayName("Son eklenen müşteriler - Tarihsel sıralama")
    void findByTenantIdAndActiveTrueOrderByCreatedAtDesc_ShouldReturnRecentFirst() {
        // When: Son eklenen müşteriler listelenir
        List<Customer> recentCustomers = customerRepository
                .findByTenantIdAndActiveTrueOrderByCreatedAtDesc(testTenant1.getId());
        
        // Then: En son eklenen en başta olur
        assertThat(recentCustomers).hasSize(2);
        assertThat(recentCustomers.get(0).getCreatedAt())
                .isAfterOrEqualTo(recentCustomers.get(1).getCreatedAt());
    }
}
