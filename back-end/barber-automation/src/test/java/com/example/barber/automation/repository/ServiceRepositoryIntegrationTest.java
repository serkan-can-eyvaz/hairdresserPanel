package com.example.barber.automation.repository;

import com.example.barber.automation.TestDataBuilder;
import com.example.barber.automation.entity.Service;
import com.example.barber.automation.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service Repository Integration Tests
 * 
 * ServiceRepository'nin JPA query'lerini ve entity ilişkilerini test eder.
 * 
 * Test kapsamı:
 * - Temel CRUD operasyonları
 * - Custom query'ler
 * - Multi-tenant izolasyon
 * - Sorting ve pagination
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ServiceRepository Integration Tests")
class ServiceRepositoryIntegrationTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private ServiceRepository serviceRepository;
    
    private Tenant testTenant1;
    private Tenant testTenant2;
    private Service service1;
    private Service service2;
    private Service inactiveService;
    
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
        
        // Test hizmetlerini oluştur
        service1 = TestDataBuilder.createTestService("Saç Kesimi", 45, new BigDecimal("150.00"), testTenant1);
        service1.setSortOrder(1);
        
        service2 = TestDataBuilder.createTestService("Sakal Tıraşı", 30, new BigDecimal("100.00"), testTenant1);
        service2.setSortOrder(2);
        
        inactiveService = TestDataBuilder.createTestService("Eski Hizmet", 60, new BigDecimal("200.00"), testTenant1);
        inactiveService.setActive(false);
        inactiveService.setSortOrder(3);
        
        // Service'leri persist et
        service1 = entityManager.persistAndFlush(service1);
        service2 = entityManager.persistAndFlush(service2);
        inactiveService = entityManager.persistAndFlush(inactiveService);
        
        // Clear cache for fresh queries
        entityManager.clear();
    }
    
    @Test
    @DisplayName("Aktif hizmetler tenant'a göre sıralı listeleme")
    void findByTenantIdAndActiveTrueOrderBySortOrder_ShouldReturnSortedActiveServices() {
        // When: Aktif hizmetler sıralı olarak listelenir
        List<Service> services = serviceRepository.findByTenantIdAndActiveTrueOrderBySortOrder(testTenant1.getId());
        
        // Then: Sadece aktif hizmetler sıralı döner
        assertThat(services).hasSize(2);
        assertThat(services.get(0).getName()).isEqualTo("Saç Kesimi");
        assertThat(services.get(1).getName()).isEqualTo("Sakal Tıraşı");
        
        // Sort order'a göre sıralı olmalı
        assertThat(services.get(0).getSortOrder()).isEqualTo(1);
        assertThat(services.get(1).getSortOrder()).isEqualTo(2);
        
        // Pasif hizmet dahil olmamalı
        assertThat(services)
                .extracting(Service::getName)
                .doesNotContain("Eski Hizmet");
    }
    
    @Test
    @DisplayName("İsim ile hizmet arama - Büyük/küçük harf duyarsız")
    void findByNameContainingIgnoreCaseAndTenantIdAndActiveTrue_ShouldFindMatches() {
        // When: İsim ile arama yapılır (küçük harfle)
        List<Service> results = serviceRepository.findByNameContainingIgnoreCaseAndTenantIdAndActiveTrue(
                "saç", testTenant1.getId());
        
        // Then: Büyük/küçük harf fark etmeden bulur
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Saç Kesimi");
        
        // Partial match test
        List<Service> partialResults = serviceRepository.findByNameContainingIgnoreCaseAndTenantIdAndActiveTrue(
                "sakal", testTenant1.getId());
        assertThat(partialResults).hasSize(1);
        assertThat(partialResults.get(0).getName()).isEqualTo("Sakal Tıraşı");
    }
    
    @Test
    @DisplayName("Süre aralığına göre hizmet filtreleme")
    void findByTenantIdAndActiveTrueAndDurationMinutesBetween_ShouldFilterByDuration() {
        // When: 30-50 dakika arası hizmetler aranır
        List<Service> results = serviceRepository.findByTenantIdAndActiveTrueAndDurationMinutesBetween(
                testTenant1.getId(), 30, 50);
        
        // Then: Süre kriterini karşılayan hizmetler döner
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(Service::getName)
                .containsExactlyInAnyOrder("Saç Kesimi", "Sakal Tıraşı");
        
        // 60 dakikalık pasif hizmet dahil olmamalı
        assertThat(results)
                .extracting(Service::getName)
                .doesNotContain("Eski Hizmet");
    }
    
    @Test
    @DisplayName("Fiyat aralığına göre hizmet filtreleme")
    void findByTenantIdAndActiveTrueAndPriceBetween_ShouldFilterByPrice() {
        // When: 80-120 TL arası hizmetler aranır
        List<Service> results = serviceRepository.findByTenantIdAndActiveTrueAndPriceBetween(
                testTenant1.getId(), new BigDecimal("80.00"), new BigDecimal("120.00"));
        
        // Then: Fiyat kriterini karşılayan hizmetler döner
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Sakal Tıraşı");
        assertThat(results.get(0).getPrice()).isEqualTo(new BigDecimal("100.00"));
    }
    
    @Test
    @DisplayName("Tenant bazlı aktif hizmet sayısı")
    void countByTenantIdAndActiveTrue_ShouldReturnCorrectCount() {
        // When: Aktif hizmet sayısı hesaplanır
        long count = serviceRepository.countByTenantIdAndActiveTrue(testTenant1.getId());
        
        // Then: Doğru sayı döner (pasif dahil değil)
        assertThat(count).isEqualTo(2);
        
        // Farklı tenant için 0 olmalı
        long otherTenantCount = serviceRepository.countByTenantIdAndActiveTrue(testTenant2.getId());
        assertThat(otherTenantCount).isEqualTo(0);
    }
    
    @Test
    @DisplayName("En yüksek sort order bulma")
    void findMaxSortOrderByTenantIdAndActiveTrue_ShouldReturnHighestOrder() {
        // When: En yüksek sort order bulunur
        Integer maxOrder = serviceRepository.findMaxSortOrderByTenantIdAndActiveTrue(testTenant1.getId());
        
        // Then: En yüksek aktif hizmetin sort order'ı döner
        assertThat(maxOrder).isEqualTo(2); // service2'nin sort order'ı
        
        // Pasif hizmetin sort order'ı (3) dahil olmamalı
    }
    
    @Test
    @DisplayName("Hizmet adı benzersizlik kontrolü - Aynı tenant içinde")
    void existsByNameAndTenantIdAndActiveTrue_ShouldCheckUniqueness() {
        // When: Aynı tenant'ta mevcut hizmet adını kontrol et
        boolean exists = serviceRepository.existsByNameAndTenantIdAndActiveTrue(
                "Saç Kesimi", testTenant1.getId());
        
        // Then: Mevcut hizmet var, true döner
        assertThat(exists).isTrue();
        
        // When: Olmayan hizmet adını kontrol et
        boolean notExists = serviceRepository.existsByNameAndTenantIdAndActiveTrue(
                "Boya", testTenant1.getId());
        
        // Then: Olmayan hizmet, false döner
        assertThat(notExists).isFalse();
        
        // When: Pasif hizmet adını kontrol et
        boolean inactiveExists = serviceRepository.existsByNameAndTenantIdAndActiveTrue(
                "Eski Hizmet", testTenant1.getId());
        
        // Then: Pasif hizmet, active=true kontrolünde false döner
        assertThat(inactiveExists).isFalse();
    }
    
    @Test
    @DisplayName("Multi-tenant izolasyon - Farklı tenant'ların hizmetlerini görmemeli")
    void findByTenantIdAndActiveTrueOrderBySortOrder_ShouldNotCrossTenantBoundaries() {
        // Given: Farklı tenant'a ait hizmet
        Service otherTenantService = TestDataBuilder.createTestService("Boya", 90, new BigDecimal("300.00"), testTenant2);
        otherTenantService.setSortOrder(1);
        entityManager.persistAndFlush(otherTenantService);
        entityManager.clear();
        
        // When: Tenant1'in hizmetleri listelenir
        List<Service> tenant1Services = serviceRepository.findByTenantIdAndActiveTrueOrderBySortOrder(testTenant1.getId());
        
        // Then: Sadece kendi hizmetlerini görür
        assertThat(tenant1Services).hasSize(2);
        assertThat(tenant1Services)
                .extracting(Service::getName)
                .containsExactlyInAnyOrder("Saç Kesimi", "Sakal Tıraşı")
                .doesNotContain("Boya");
        
        // When: Tenant2'nin hizmetleri listelenir
        List<Service> tenant2Services = serviceRepository.findByTenantIdAndActiveTrueOrderBySortOrder(testTenant2.getId());
        
        // Then: Sadece kendi hizmetini görür
        assertThat(tenant2Services).hasSize(1);
        assertThat(tenant2Services.get(0).getName()).isEqualTo("Boya");
    }
    
    @Test
    @DisplayName("Hızlı hizmetler - 30 dakika altı")
    void findByTenantIdAndActiveTrueAndDurationMinutesLessThan_ShouldReturnQuickServices() {
        // Given: 20 dakikalık hızlı hizmet ekle
        Service quickService = TestDataBuilder.createTestService("Hızlı Kesim", 20, new BigDecimal("80.00"), testTenant1);
        quickService.setSortOrder(4);
        entityManager.persistAndFlush(quickService);
        entityManager.clear();
        
        // When: 30 dakika altı hizmetler aranır
        List<Service> quickServices = serviceRepository.findByTenantIdAndActiveTrueAndDurationMinutesLessThan(
                testTenant1.getId(), 30);
        
        // Then: Sadece hızlı hizmetler döner
        assertThat(quickServices).hasSize(1);
        assertThat(quickServices.get(0).getName()).isEqualTo("Hızlı Kesim");
        assertThat(quickServices.get(0).getDurationMinutes()).isEqualTo(20);
    }
    
    @Test
    @DisplayName("Popüler fiyat aralığı - Ortalama fiyat hesaplama")
    void findAveragePriceByTenantIdAndActiveTrue_ShouldCalculateAveragePrice() {
        // When: Ortalama fiyat hesaplanır
        Double averagePrice = serviceRepository.findAveragePriceByTenantIdAndActiveTrue(testTenant1.getId());
        
        // Then: Doğru ortalama döner (150 + 100) / 2 = 125
        assertThat(averagePrice).isEqualTo(125.0);
    }
}
