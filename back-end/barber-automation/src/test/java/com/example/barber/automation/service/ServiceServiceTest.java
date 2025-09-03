package com.example.barber.automation.service;

import com.example.barber.automation.TestDataBuilder;
import com.example.barber.automation.dto.ServiceDto;
import com.example.barber.automation.entity.Service;
import com.example.barber.automation.entity.Tenant;
import com.example.barber.automation.repository.ServiceRepository;
import com.example.barber.automation.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ServiceService Unit Test
 * 
 * Bu test sınıfı ServiceService'in business logic'ini test eder:
 * - Hizmet oluşturma ve validation kuralları
 * - Hizmet adı tekrar kontrolü (tenant bazlı)
 * - Hizmet güncelleme ve aktivasyon/deaktivasyon
 * - Hizmet sıralama yönetimi
 * - Multi-tenant hizmet yönetimi
 * - WhatsApp bot için hizmet listesi formatı
 * - Popüler hizmet analizi
 * 
 * ServiceService, kuaförlerin hizmet kataloğunu yönetmek için kullanılır
 * ve WhatsApp bot'unun müşterilere göstereceği hizmet listesini sağlar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceService Unit Tests")
class ServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private ServiceService serviceService;

    // Test data
    private Tenant testTenant;
    private Service existingService;
    private ServiceDto serviceDto;

    @BeforeEach
    void setUp() {
        // Test tenant
        testTenant = TestDataBuilder.createDefaultTestTenant();
        testTenant.setId(1L);

        // Mevcut hizmet
        existingService = TestDataBuilder.createDefaultHairCutService(testTenant);
        existingService.setId(1L);

        // Test DTO
        serviceDto = new ServiceDto();
        serviceDto.setName("Yeni Hizmet");
        serviceDto.setDescription("Yeni hizmet açıklaması");
        serviceDto.setDurationMinutes(60);
        serviceDto.setPrice(new BigDecimal("200.00"));
        serviceDto.setCurrency("TRY");
        serviceDto.setSortOrder(1);
    }

    @Test
    @DisplayName("Hizmet ID'ye göre bulma - Tenant isolasyonu")
    void findById_WithTenantIsolation_ShouldReturnService() {
        // Given: Belirli tenant'a ait aktif hizmet
        existingService.setActive(true);
        when(serviceRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(existingService));

        // When: Service çağrılır
        Optional<ServiceDto> result = serviceService.findById(1L, 1L);

        // Then: Doğru hizmet döner
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getName()).isEqualTo(existingService.getName());
        assertThat(result.get().getTenantId()).isEqualTo(1L);

        verify(serviceRepository).findByIdAndTenantId(1L, 1L);
    }

    @Test
    @DisplayName("Hizmet ID'ye göre bulma - Pasif hizmet filtrelenir")
    void findById_WhenInactiveService_ShouldReturnEmpty() {
        // Given: Pasif hizmet
        existingService.setActive(false);
        when(serviceRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(existingService));

        // When: Service çağrılır
        Optional<ServiceDto> result = serviceService.findById(1L, 1L);

        // Then: Pasif hizmet filtrelenir
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Tenant'a ait aktif hizmetleri listeleme - Sıralı")
    void findAllByTenant_ShouldReturnSortedActiveServices() {
        // Given: Sıralı hizmetler
        Service service1 = TestDataBuilder.createTestService("Hizmet 1", 30, new BigDecimal("100"), testTenant);
        service1.setSortOrder(1);
        Service service2 = TestDataBuilder.createTestService("Hizmet 2", 45, new BigDecimal("150"), testTenant);
        service2.setSortOrder(2);

        when(serviceRepository.findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(1L))
                .thenReturn(Arrays.asList(service1, service2));

        // When: Service çağrılır
        List<ServiceDto> result = serviceService.findAllByTenant(1L);

        // Then: Sıralı hizmetler döner
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ServiceDto::getName)
                .containsExactly("Hizmet 1", "Hizmet 2"); // Sıralı

        verify(serviceRepository).findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(1L);
    }

    @Test
    @DisplayName("Tenant'a ait tüm hizmetleri listeleme - Pasif dahil")
    void findAllByTenantIncludingInactive_ShouldReturnAllServices() {
        // Given: Aktif ve pasif hizmetler
        Service activeService = TestDataBuilder.createTestService("Aktif", 30, new BigDecimal("100"), testTenant);
        activeService.setActive(true);
        Service inactiveService = TestDataBuilder.createTestService("Pasif", 45, new BigDecimal("150"), testTenant);
        inactiveService.setActive(false);

        when(serviceRepository.findByTenantIdOrderBySortOrderAscNameAsc(1L))
                .thenReturn(Arrays.asList(activeService, inactiveService));

        // When: Service çağrılır
        List<ServiceDto> result = serviceService.findAllByTenantIncludingInactive(1L);

        // Then: Tüm hizmetler döner
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ServiceDto::getName)
                .containsExactlyInAnyOrder("Aktif", "Pasif");

        verify(serviceRepository).findByTenantIdOrderBySortOrderAscNameAsc(1L);
    }

    @Test
    @DisplayName("Yeni hizmet oluşturma - Başarılı senaryo")
    void createService_WithValidData_ShouldCreateService() {
        // Given: Geçerli tenant ve müsait hizmet adı
        serviceDto.setSortOrder(null); // Otomatik sıra numarası için
        
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(testTenant));
        when(serviceRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(1L, serviceDto.getName(), 0L))
                .thenReturn(false);
        when(serviceRepository.countByTenantIdAndActiveTrue(1L))
                .thenReturn(2L); // Mevcut 2 hizmet var

        Service savedService = new Service();
        savedService.setId(1L);
        savedService.setName(serviceDto.getName());
        savedService.setDurationMinutes(serviceDto.getDurationMinutes());
        savedService.setPrice(serviceDto.getPrice());
        savedService.setTenant(testTenant);
        savedService.setActive(true);
        savedService.setSortOrder(3); // Yeni hizmet sıra numarası

        when(serviceRepository.save(any(Service.class)))
                .thenReturn(savedService);

        // When: Service çağrılır
        ServiceDto result = serviceService.createService(serviceDto, 1L);

        // Then: Hizmet oluşturulur
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo(serviceDto.getName());
        assertThat(result.getActive()).isTrue();
        assertThat(result.getSortOrder()).isEqualTo(3); // Otomatik sıra numarası

        verify(tenantRepository).findById(1L);
        verify(serviceRepository).countByTenantIdAndActiveTrue(1L);
        verify(serviceRepository).save(any(Service.class));
    }

    @Test
    @DisplayName("Yeni hizmet oluşturma - Geçersiz tenant")
    void createService_WithInvalidTenant_ShouldThrowException() {
        // Given: Geçersiz tenant ID
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.empty());

        // When & Then: Exception fırlatılır
        assertThatThrownBy(() -> serviceService.createService(serviceDto, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kuaför bulunamadı");

        verify(serviceRepository, never()).save(any(Service.class));
    }

    @Test
    @DisplayName("Yeni hizmet oluşturma - Hizmet adı tekrar hatası")
    void createService_WithDuplicateName_ShouldThrowException() {
        // Given: Hizmet adı zaten kayıtlı
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(testTenant));
        when(serviceRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(1L, serviceDto.getName(), 0L))
                .thenReturn(true);

        // When & Then: Exception fırlatılır
        assertThatThrownBy(() -> serviceService.createService(serviceDto, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hizmet adı zaten kayıtlı");

        verify(serviceRepository, never()).save(any(Service.class));
    }

    @Test
    @DisplayName("Hizmet güncelleme - Başarılı senaryo")
    void updateService_WithValidData_ShouldUpdateService() {
        // Given: Mevcut hizmet ve valid data
        when(serviceRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(existingService));
        when(serviceRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(1L, serviceDto.getName(), 1L))
                .thenReturn(false);

        Service updatedService = new Service();
        updatedService.setId(1L);
        updatedService.setName(serviceDto.getName());
        updatedService.setDescription(serviceDto.getDescription());
        updatedService.setDurationMinutes(serviceDto.getDurationMinutes());
        updatedService.setPrice(serviceDto.getPrice());
        updatedService.setTenant(testTenant);

        when(serviceRepository.save(any(Service.class)))
                .thenReturn(updatedService);

        // When: Service çağrılır
        ServiceDto result = serviceService.updateService(1L, serviceDto);

        // Then: Hizmet güncellenir
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(serviceDto.getName());
        assertThat(result.getDescription()).isEqualTo(serviceDto.getDescription());
        assertThat(result.getDurationMinutes()).isEqualTo(serviceDto.getDurationMinutes());
        assertThat(result.getPrice()).isEqualTo(serviceDto.getPrice());

        verify(serviceRepository).save(any(Service.class));
    }

    @Test
    @DisplayName("Hizmet güncelleme - Hizmet adı başka hizmet tarafından kullanılıyor")
    void updateService_WithDuplicateName_ShouldThrowException() {
        // Given: Mevcut hizmet var ama hizmet adı başkasında
        when(serviceRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(existingService));
        when(serviceRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(1L, serviceDto.getName(), 1L))
                .thenReturn(true);

        // When & Then: Exception fırlatılır
        assertThatThrownBy(() -> serviceService.updateService(1L, serviceDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hizmet adı başka bir hizmet tarafından kullanılıyor");

        verify(serviceRepository, never()).save(any(Service.class));
    }

    @Test
    @DisplayName("Hizmet deaktivasyon")
    void deactivateService_WhenExists_ShouldSetInactive() {
        // Given: Mevcut hizmet
        when(serviceRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(existingService));

        // When: Deaktivasyon
        serviceService.deactivateService(1L, 1L);

        // Then: Active false set edilir
        verify(serviceRepository).findByIdAndTenantId(1L, 1L);
        verify(serviceRepository).save(any(Service.class));
        
        assertThat(existingService.getActive()).isFalse();
    }

    @Test
    @DisplayName("Hizmet aktivasyon")
    void activateService_WhenExists_ShouldSetActive() {
        // Given: Pasif hizmet
        existingService.setActive(false);
        when(serviceRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(existingService));

        // When: Aktivasyon
        serviceService.activateService(1L, 1L);

        // Then: Active true set edilir
        verify(serviceRepository).findByIdAndTenantId(1L, 1L);
        verify(serviceRepository).save(any(Service.class));
        
        assertThat(existingService.getActive()).isTrue();
    }

    @Test
    @DisplayName("Hizmet sıralaması güncelleme")
    void updateServiceOrder_WhenExists_ShouldUpdateSortOrder() {
        // Given: Mevcut hizmet
        when(serviceRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(existingService));

        // When: Sıralama güncellenir
        serviceService.updateServiceOrder(1L, 5, 1L);

        // Then: Sort order güncellenir
        verify(serviceRepository).findByIdAndTenantId(1L, 1L);
        verify(serviceRepository).save(any(Service.class));
        
        assertThat(existingService.getSortOrder()).isEqualTo(5);
    }

    @Test
    @DisplayName("Hizmet adına göre arama")
    void searchByName_ShouldCallRepositoryWithCorrectParameter() {
        // Given: Repository'den sonuç döner
        List<Service> services = Arrays.asList(existingService);
        when(serviceRepository.findByTenantIdAndNameContainingIgnoreCaseAndActiveTrue(1L, "saç"))
                .thenReturn(services);

        // When: Service çağrılır
        List<ServiceDto> result = serviceService.searchByName("saç", 1L);

        // Then: DTO listesi döner
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo(existingService.getName());

        verify(serviceRepository).findByTenantIdAndNameContainingIgnoreCaseAndActiveTrue(1L, "saç");
    }

    @Test
    @DisplayName("Süre aralığına göre hizmet bulma")
    void findByDurationRange_ShouldReturnServicesInRange() {
        // Given: Belirli süre aralığındaki hizmetler
        when(serviceRepository.findByTenantIdAndDurationBetween(1L, 30, 60))
                .thenReturn(Arrays.asList(existingService));

        // When: Service çağrılır
        List<ServiceDto> result = serviceService.findByDurationRange(30, 60, 1L);

        // Then: Aralıktaki hizmetler döner
        assertThat(result).hasSize(1);
        verify(serviceRepository).findByTenantIdAndDurationBetween(1L, 30, 60);
    }

    @Test
    @DisplayName("En popüler hizmetleri getirme")
    void findMostPopularServices_ShouldReturnTop5() {
        // Given: Popüler hizmetler
        List<Service> popularServices = Arrays.asList(
                TestDataBuilder.createTestService("Popüler 1", 30, new BigDecimal("100"), testTenant),
                TestDataBuilder.createTestService("Popüler 2", 45, new BigDecimal("150"), testTenant)
        );
        when(serviceRepository.findMostPopularServicesByTenantId(1L))
                .thenReturn(popularServices);

        // When: Service çağrılır
        List<ServiceDto> result = serviceService.findMostPopularServices(1L);

        // Then: Popüler hizmetler döner (limit 5)
        assertThat(result).hasSize(2);
        verify(serviceRepository).findMostPopularServicesByTenantId(1L);
    }

    @Test
    @DisplayName("Hizmet sayısı")
    void countByTenant_ShouldReturnRepositoryCount() {
        // Given: Repository'den sayı döner
        when(serviceRepository.countByTenantIdAndActiveTrue(1L))
                .thenReturn(8L);

        // When: Service çağrılır
        long result = serviceService.countByTenant(1L);

        // Then: Doğru sayı döner
        assertThat(result).isEqualTo(8L);
        verify(serviceRepository).countByTenantIdAndActiveTrue(1L);
    }

    @Test
    @DisplayName("WhatsApp bot için hizmet listesi formatı")
    void getServicesForWhatsApp_ShouldReturnFormattedList() {
        // Given: Hizmet listesi
        Service service1 = TestDataBuilder.createTestService("Saç Kesimi", 45, new BigDecimal("150"), testTenant);
        service1.setDescription("Profesyonel saç kesimi");
        Service service2 = TestDataBuilder.createTestService("Sakal Tıraşı", 30, new BigDecimal("100"), testTenant);

        when(serviceRepository.findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(1L))
                .thenReturn(Arrays.asList(service1, service2));

        // When: Service çağrılır
        String result = serviceService.getServicesForWhatsApp(1L);

        // Then: Formatted liste döner
        assertThat(result).contains("🔸 *Hizmetlerimiz:*");
        assertThat(result).contains("1. *Saç Kesimi*");
        assertThat(result).contains("2. *Sakal Tıraşı*");
        assertThat(result).contains("⏱️ 45 dakika");
        assertThat(result).contains("💰 150 TRY");
        assertThat(result).contains("📝 Profesyonel saç kesimi");

        verify(serviceRepository).findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(1L);
    }

    @Test
    @DisplayName("WhatsApp bot için hizmet listesi - Hizmet yok")
    void getServicesForWhatsApp_WhenNoServices_ShouldReturnMessage() {
        // Given: Hiç hizmet yok
        when(serviceRepository.findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(1L))
                .thenReturn(Arrays.asList());

        // When: Service çağrılır
        String result = serviceService.getServicesForWhatsApp(1L);

        // Then: Uygun mesaj döner
        assertThat(result).isEqualTo("Henüz hizmet tanımlanmamış.");
        verify(serviceRepository).findByTenantIdAndActiveTrueOrderBySortOrderAscNameAsc(1L);
    }

    @Test
    @DisplayName("Entity to DTO conversion test")
    void convertToDto_ShouldMapAllFields() {
        // Given: Tam dolu service entity
        existingService.setId(1L);
        existingService.setName("Test Hizmet");
        existingService.setDescription("Test açıklama");
        existingService.setDurationMinutes(60);
        existingService.setPrice(new BigDecimal("250.00"));
        existingService.setCurrency("TRY");
        existingService.setActive(true);
        existingService.setSortOrder(3);

        when(serviceRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(existingService));

        // When: Service çağrılır
        Optional<ServiceDto> result = serviceService.findById(1L, 1L);

        // Then: Tüm alanlar doğru map edilir
        assertThat(result).isPresent();
        ServiceDto dto = result.get();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Test Hizmet");
        assertThat(dto.getDescription()).isEqualTo("Test açıklama");
        assertThat(dto.getDurationMinutes()).isEqualTo(60);
        assertThat(dto.getPrice()).isEqualTo(new BigDecimal("250.00"));
        assertThat(dto.getCurrency()).isEqualTo("TRY");
        assertThat(dto.getActive()).isTrue();
        assertThat(dto.getSortOrder()).isEqualTo(3);
        assertThat(dto.getTenantId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Hizmet oluşturma - Sıra numarası otomatik atama")
    void createService_WithoutSortOrder_ShouldAutoAssignOrder() {
        // Given: Sort order belirtilmemiş
        serviceDto.setSortOrder(null);
        
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(testTenant));
        when(serviceRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(eq(1L), anyString(), eq(0L)))
                .thenReturn(false);
        when(serviceRepository.countByTenantIdAndActiveTrue(1L))
                .thenReturn(3L); // 3 mevcut hizmet

        Service savedService = new Service();
        savedService.setId(1L);
        savedService.setName(serviceDto.getName());
        savedService.setTenant(testTenant);
        savedService.setSortOrder(4); // Otomatik atanan

        when(serviceRepository.save(any(Service.class)))
                .thenReturn(savedService);

        // When: Service çağrılır
        ServiceDto result = serviceService.createService(serviceDto, 1L);

        // Then: Otomatik sıra numarası atanır
        assertThat(result.getSortOrder()).isEqualTo(4); // 3 + 1
        verify(serviceRepository).countByTenantIdAndActiveTrue(1L);
    }
}
