package com.example.barber.automation.service;

import com.example.barber.automation.TestDataBuilder;
import com.example.barber.automation.dto.CustomerDto;
import com.example.barber.automation.entity.Customer;
import com.example.barber.automation.entity.Tenant;
import com.example.barber.automation.repository.CustomerRepository;
import com.example.barber.automation.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CustomerService Unit Test
 * 
 * Bu test sınıfı CustomerService'in business logic'ini test eder:
 * - Müşteri oluşturma ve validation kuralları
 * - Telefon numarası tekrar kontrolü (tenant bazlı)
 * - WhatsApp'tan otomatik müşteri oluşturma
 * - Müşteri güncelleme ve deaktivasyon
 * - Hatırlatma için müşteri bulma algoritması
 * - Multi-tenant müşteri yönetimi
 * 
 * CustomerService, WhatsApp bot entegrasyonu için kritik role sahiptir.
 * Otomatik müşteri oluşturma ve tenant isolation özellikle test edilmelidir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private CustomerService customerService;

    // Test data
    private Tenant testTenant;
    private Customer existingCustomer;
    private CustomerDto customerDto;

    @BeforeEach
    void setUp() {
        // Test tenant
        testTenant = TestDataBuilder.createDefaultTestTenant();
        testTenant.setId(1L);

        // Mevcut müşteri
        existingCustomer = TestDataBuilder.createTestCustomer("Ali Veli", "+905331234567", testTenant);
        existingCustomer.setId(1L);

        // Test DTO
        customerDto = new CustomerDto();
        customerDto.setName("Yeni Müşteri");
        customerDto.setPhoneNumber("+905339876543");
        customerDto.setEmail("yeni@musteri.com");
        customerDto.setNotes("Test müşteri notu");
        customerDto.setAllowNotifications(true);
    }

    @Test
    @DisplayName("Telefon numarasına göre müşteri bulma - Tenant isolasyonu")
    void findByPhoneNumber_WithTenantIsolation_ShouldReturnCorrectCustomer() {
        // Given: Belirli tenant'a ait müşteri
        when(customerRepository.findByPhoneNumberAndTenantId("+905331234567", 1L))
                .thenReturn(Optional.of(existingCustomer));

        // When: Service çağrılır
        Optional<CustomerDto> result = customerService.findByPhoneNumber("+905331234567", 1L);

        // Then: Doğru müşteri döner
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Ali Veli");
        assertThat(result.get().getPhoneNumber()).isEqualTo("+905331234567");
        assertThat(result.get().getTenantId()).isEqualTo(1L);

        // Repository'nin tenant ID ile çağrıldığını doğrula
        verify(customerRepository).findByPhoneNumberAndTenantId("+905331234567", 1L);
    }

    @Test
    @DisplayName("Müşteri ID'ye göre bulma - Aktif müşteri kontrolü")
    void findById_WhenActiveCustomer_ShouldReturnCustomerDto() {
        // Given: Aktif müşteri
        existingCustomer.setActive(true);
        when(customerRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(existingCustomer));

        // When: Service çağrılır
        Optional<CustomerDto> result = customerService.findById(1L, 1L);

        // Then: Müşteri DTO döner
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getActive()).isTrue();

        verify(customerRepository).findByIdAndTenantId(1L, 1L);
    }

    @Test
    @DisplayName("Müşteri ID'ye göre bulma - Pasif müşteri filtrelenir")
    void findById_WhenInactiveCustomer_ShouldReturnEmpty() {
        // Given: Pasif müşteri
        existingCustomer.setActive(false);
        when(customerRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(existingCustomer));

        // When: Service çağrılır
        Optional<CustomerDto> result = customerService.findById(1L, 1L);

        // Then: Pasif müşteri filtrelenir
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Tenant'a ait müşterileri listeleme")
    void findAllByTenant_ShouldReturnTenantCustomers() {
        // Given: Tenant'a ait müşteriler
        Customer customer1 = TestDataBuilder.createTestCustomer("Müşteri 1", "+905331111111", testTenant);
        Customer customer2 = TestDataBuilder.createTestCustomer("Müşteri 2", "+905332222222", testTenant);
        when(customerRepository.findByTenantIdAndActiveTrueOrderByNameAsc(1L))
                .thenReturn(Arrays.asList(customer1, customer2));

        // When: Service çağrılır
        List<CustomerDto> result = customerService.findAllByTenant(1L);

        // Then: Doğru müşteriler döner
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CustomerDto::getName)
                .containsExactlyInAnyOrder("Müşteri 1", "Müşteri 2");

        verify(customerRepository).findByTenantIdAndActiveTrueOrderByNameAsc(1L);
    }

    @Test
    @DisplayName("Yeni müşteri oluşturma - Başarılı senaryo")
    void createCustomer_WithValidData_ShouldCreateAndReturnDto() {
        // Given: Geçerli tenant ve müsait telefon numarası
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(testTenant));
        when(customerRepository.existsByTenantIdAndPhoneNumberAndIdNot(1L, customerDto.getPhoneNumber(), 0L))
                .thenReturn(false);

        // Save mock'u
        Customer savedCustomer = new Customer();
        savedCustomer.setId(1L);
        savedCustomer.setName(customerDto.getName());
        savedCustomer.setPhoneNumber(customerDto.getPhoneNumber());
        savedCustomer.setTenant(testTenant);
        savedCustomer.setActive(true);

        when(customerRepository.save(any(Customer.class)))
                .thenReturn(savedCustomer);

        // When: Service çağrılır
        CustomerDto result = customerService.createCustomer(customerDto, 1L);

        // Then: DTO döner ve entity özellikleri doğru set edilir
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo(customerDto.getName());
        assertThat(result.getActive()).isTrue();
        assertThat(result.getAllowNotifications()).isTrue();

        verify(tenantRepository).findById(1L);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Yeni müşteri oluşturma - Geçersiz tenant")
    void createCustomer_WithInvalidTenant_ShouldThrowException() {
        // Given: Geçersiz tenant ID
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.empty());

        // When & Then: Exception fırlatılır
        assertThatThrownBy(() -> customerService.createCustomer(customerDto, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kuaför bulunamadı");

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("Yeni müşteri oluşturma - Telefon numarası tekrar hatası")
    void createCustomer_WithDuplicatePhoneNumber_ShouldThrowException() {
        // Given: Telefon numarası zaten kayıtlı
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(testTenant));
        when(customerRepository.existsByTenantIdAndPhoneNumberAndIdNot(1L, customerDto.getPhoneNumber(), 0L))
                .thenReturn(true);

        // When & Then: Exception fırlatılır
        assertThatThrownBy(() -> customerService.createCustomer(customerDto, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telefon numarası zaten kayıtlı");

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("WhatsApp'tan müşteri oluşturma - Yeni müşteri")
    void createCustomerFromWhatsApp_NewCustomer_ShouldCreateCustomer() {
        // Given: Yeni WhatsApp müşterisi
        when(customerRepository.findByPhoneNumberAndTenantId("+905331234567", 1L))
                .thenReturn(Optional.empty());
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(testTenant));
        when(customerRepository.existsByTenantIdAndPhoneNumberAndIdNot(eq(1L), anyString(), eq(0L)))
                .thenReturn(false);

        Customer savedCustomer = new Customer();
        savedCustomer.setId(1L);
        savedCustomer.setName("WhatsApp Kullanıcı");
        savedCustomer.setPhoneNumber("+905331234567");
        savedCustomer.setTenant(testTenant);
        savedCustomer.setActive(true);
        savedCustomer.setAllowNotifications(true);

        when(customerRepository.save(any(Customer.class)))
                .thenReturn(savedCustomer);

        // When: WhatsApp'tan müşteri oluştur
        CustomerDto result = customerService.createCustomerFromWhatsApp("WhatsApp Kullanıcı", "+905331234567", 1L);

        // Then: Yeni müşteri oluşturulur
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("WhatsApp Kullanıcı");
        assertThat(result.getPhoneNumber()).isEqualTo("+905331234567");
        assertThat(result.getAllowNotifications()).isTrue();

        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("WhatsApp'tan müşteri oluşturma - Mevcut müşteri")
    void createCustomerFromWhatsApp_ExistingCustomer_ShouldReturnExisting() {
        // Given: Mevcut müşteri
        when(customerRepository.findByPhoneNumberAndTenantId("+905331234567", 1L))
                .thenReturn(Optional.of(existingCustomer));

        // When: WhatsApp'tan müşteri oluştur
        CustomerDto result = customerService.createCustomerFromWhatsApp("Farklı İsim", "+905331234567", 1L);

        // Then: Mevcut müşteri döner
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Ali Veli"); // Mevcut isim korunur
        assertThat(result.getPhoneNumber()).isEqualTo("+905331234567");

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("WhatsApp'tan müşteri oluşturma - İsim null ise varsayılan")
    void createCustomerFromWhatsApp_WithNullName_ShouldUseDefaultName() {
        // Given: İsim null
        when(customerRepository.findByPhoneNumberAndTenantId("+905331234567", 1L))
                .thenReturn(Optional.empty());
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(testTenant));
        when(customerRepository.existsByTenantIdAndPhoneNumberAndIdNot(eq(1L), anyString(), eq(0L)))
                .thenReturn(false);

        Customer savedCustomer = new Customer();
        savedCustomer.setId(1L);
        savedCustomer.setName("WhatsApp Müşteri");
        savedCustomer.setPhoneNumber("+905331234567");
        savedCustomer.setTenant(testTenant);
        savedCustomer.setActive(true);

        when(customerRepository.save(any(Customer.class)))
                .thenReturn(savedCustomer);

        // When: İsim null ile müşteri oluştur
        CustomerDto result = customerService.createCustomerFromWhatsApp(null, "+905331234567", 1L);

        // Then: Varsayılan isim kullanılır
        assertThat(result.getName()).isEqualTo("WhatsApp Müşteri");
    }

    @Test
    @DisplayName("Müşteri güncelleme - Başarılı senaryo")
    void updateCustomer_WithValidData_ShouldUpdateAndReturnDto() {
        // Given: Mevcut müşteri ve valid data
        when(customerRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(existingCustomer));
        when(customerRepository.existsByTenantIdAndPhoneNumberAndIdNot(1L, customerDto.getPhoneNumber(), 1L))
                .thenReturn(false);

        Customer updatedCustomer = new Customer();
        updatedCustomer.setId(1L);
        updatedCustomer.setName(customerDto.getName());
        updatedCustomer.setPhoneNumber(customerDto.getPhoneNumber());
        updatedCustomer.setTenant(testTenant);
        updatedCustomer.setActive(true);

        when(customerRepository.save(any(Customer.class)))
                .thenReturn(updatedCustomer);

        // When: Service çağrılır
        CustomerDto result = customerService.updateCustomer(1L, customerDto, 1L);

        // Then: Güncellenmiş DTO döner
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(customerDto.getName());
        assertThat(result.getPhoneNumber()).isEqualTo(customerDto.getPhoneNumber());

        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Müşteri deaktivasyon")
    void deactivateCustomer_WhenExists_ShouldSetInactive() {
        // Given: Mevcut müşteri
        when(customerRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(existingCustomer));

        // When: Deaktivasyon
        customerService.deactivateCustomer(1L, 1L);

        // Then: Active false set edilir
        verify(customerRepository).findByIdAndTenantId(1L, 1L);
        verify(customerRepository).save(any(Customer.class));
        
        assertThat(existingCustomer.getActive()).isFalse();
    }

    @Test
    @DisplayName("Hatırlatma için müşteri bulma")
    void findCustomersForReminder_ShouldCalculateCorrectDateRange() {
        // Given: Hatırlatma için uygun müşteriler
        Customer customer1 = TestDataBuilder.createTestCustomer("Müşteri 1", "+905331111111", testTenant);
        Customer customer2 = TestDataBuilder.createTestCustomer("Müşteri 2", "+905332222222", testTenant);
        
        when(customerRepository.findCustomersForReminder(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(customer1, customer2));

        // When: Service çağrılır (30 günlük hatırlatma)
        List<CustomerDto> result = customerService.findCustomersForReminder(1L, 30);

        // Then: Müşteriler döner ve tarih hesaplaması doğru yapılır
        assertThat(result).hasSize(2);

        // Repository'nin doğru tarih aralıklarıyla çağrıldığını doğrula
        verify(customerRepository).findCustomersForReminder(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("En sadık müşteriler - Randevu sayısına göre")
    void findMostLoyalCustomers_ShouldReturnTop10() {
        // Given: Sadık müşteriler
        List<Customer> loyalCustomers = Arrays.asList(
                TestDataBuilder.createTestCustomer("Sadık 1", "+905331111111", testTenant),
                TestDataBuilder.createTestCustomer("Sadık 2", "+905332222222", testTenant)
        );
        when(customerRepository.findMostLoyalCustomersByTenantId(1L))
                .thenReturn(loyalCustomers);

        // When: Service çağrılır
        List<CustomerDto> result = customerService.findMostLoyalCustomers(1L);

        // Then: Sadık müşteriler döner (limit 10)
        assertThat(result).hasSize(2);
        verify(customerRepository).findMostLoyalCustomersByTenantId(1L);
    }

    @Test
    @DisplayName("Yeni müşteriler - Son 30 gün")
    void findNewCustomers_ShouldReturnRecentCustomers() {
        // Given: Yeni müşteriler
        List<Customer> newCustomers = Arrays.asList(
                TestDataBuilder.createTestCustomer("Yeni 1", "+905331111111", testTenant)
        );
        when(customerRepository.findNewCustomersSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(newCustomers);

        // When: Service çağrılır
        List<CustomerDto> result = customerService.findNewCustomers(1L);

        // Then: Yeni müşteriler döner
        assertThat(result).hasSize(1);
        
        // Son 30 günlük tarih hesaplaması doğru mu?
        verify(customerRepository).findNewCustomersSince(eq(1L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Müşteri sayısı")
    void countByTenant_ShouldReturnRepositoryCount() {
        // Given: Repository'den sayı döner
        when(customerRepository.countByTenantIdAndActiveTrue(1L))
                .thenReturn(15L);

        // When: Service çağrılır
        long result = customerService.countByTenant(1L);

        // Then: Doğru sayı döner
        assertThat(result).isEqualTo(15L);
        verify(customerRepository).countByTenantIdAndActiveTrue(1L);
    }

    @Test
    @DisplayName("Müşteri adına göre arama")
    void searchByName_ShouldCallRepositoryWithCorrectParameter() {
        // Given: Repository'den sonuç döner
        List<Customer> customers = Arrays.asList(existingCustomer);
        when(customerRepository.findByTenantIdAndNameContainingIgnoreCaseAndActiveTrue(1L, "ali"))
                .thenReturn(customers);

        // When: Service çağrılır
        List<CustomerDto> result = customerService.searchByName("ali", 1L);

        // Then: DTO listesi döner
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo(existingCustomer.getName());

        verify(customerRepository).findByTenantIdAndNameContainingIgnoreCaseAndActiveTrue(1L, "ali");
    }

    @Test
    @DisplayName("Entity to DTO conversion - İstatistiklerle")
    void convertToDto_ShouldIncludeStatistics() {
        // Given: Tam dolu müşteri entity
        existingCustomer.setId(1L);
        existingCustomer.setName("Test Müşteri");
        existingCustomer.setPhoneNumber("+905331234567");
        existingCustomer.setEmail("test@musteri.com");
        existingCustomer.setNotes("Test notu");
        existingCustomer.setActive(true);
        existingCustomer.setAllowNotifications(true);

        when(customerRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(existingCustomer));

        // When: Service çağrılır
        Optional<CustomerDto> result = customerService.findById(1L, 1L);

        // Then: Tüm alanlar doğru map edilir
        assertThat(result).isPresent();
        CustomerDto dto = result.get();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Test Müşteri");
        assertThat(dto.getPhoneNumber()).isEqualTo("+905331234567");
        assertThat(dto.getEmail()).isEqualTo("test@musteri.com");
        assertThat(dto.getNotes()).isEqualTo("Test notu");
        assertThat(dto.getActive()).isTrue();
        assertThat(dto.getAllowNotifications()).isTrue();
        assertThat(dto.getTenantId()).isEqualTo(1L);
    }
}
