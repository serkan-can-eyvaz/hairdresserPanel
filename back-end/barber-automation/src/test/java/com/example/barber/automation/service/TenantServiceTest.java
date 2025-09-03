package com.example.barber.automation.service;

import com.example.barber.automation.TestDataBuilder;
import com.example.barber.automation.dto.TenantDto;
import com.example.barber.automation.entity.Tenant;
import com.example.barber.automation.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TenantService Unit Test
 * 
 * Bu test sınıfı TenantService'in business logic'ini test eder:
 * - Kuaför oluşturma ve validation kuralları
 * - Telefon numarası tekrar kontrolü
 * - Email tekrar kontrolü
 * - Kuaför güncelleme operasyonları
 * - Kuaför aktivasyon/deaktivasyon
 * - WhatsApp entegrasyonu için tenant_id bulma
 * 
 * Unit testlerde sadece business logic test edilir, database'e hiç dokunulmaz.
 * Tüm repository çağrıları mock'lanır.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantService Unit Tests")
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    // Test data
    private Tenant existingTenant;
    private TenantDto tenantDto;

    @BeforeEach
    void setUp() {
        // Mevcut tenant oluştur
        existingTenant = TestDataBuilder.createDefaultTestTenant();
        existingTenant.setId(1L);

        // Test DTO oluştur
        tenantDto = new TenantDto();
        tenantDto.setName("Yeni Kuaför");
        tenantDto.setPhoneNumber("+905321234567");
        tenantDto.setAddress("Test Adres");
        tenantDto.setEmail("yeni@kuafor.com");
    }

    @Test
    @DisplayName("Telefon numarasına göre kuaför bulma - Başarılı senaryo")
    void findByPhoneNumber_WhenExists_ShouldReturnTenantDto() {
        // Given: Repository'den tenant döner
        when(tenantRepository.findByPhoneNumber("+905321234567"))
                .thenReturn(Optional.of(existingTenant));

        // When: Service çağrılır
        Tenant result = tenantService.findByPhoneNumber("+905321234567");

        // Then: Entity formatında sonuç döner
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(existingTenant.getName());
        assertThat(result.getPhoneNumber()).isEqualTo(existingTenant.getPhoneNumber());
        assertThat(result.getActive()).isTrue();

        // Repository'nin doğru parametrelerle çağrıldığını doğrula
        verify(tenantRepository).findByPhoneNumber("+905321234567");
    }

    @Test
    @DisplayName("Telefon numarasına göre kuaför bulma - Bulunamayan senaryo")
    void findByPhoneNumber_WhenNotExists_ShouldReturnEmpty() {
        // Given: Repository'den empty döner
        when(tenantRepository.findByPhoneNumber(anyString()))
                .thenReturn(Optional.empty());

        // When: Service çağrılır
        Tenant result = tenantService.findByPhoneNumber("+905329999999");

        // Then: Null sonuç döner
        assertThat(result).isNull();

        verify(tenantRepository).findByPhoneNumber("+905329999999");
    }

    @Test
    @DisplayName("Kuaför ID'ye göre bulma - Aktif kuaför")
    void findById_WhenExistsAndActive_ShouldReturnTenantDto() {
        // Given: Aktif tenant
        existingTenant.setActive(true);
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(existingTenant));

        // When: Service çağrılır
        Optional<TenantDto> result = tenantService.findById(1L);

        // Then: Sonuç döner
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getActive()).isTrue();
    }

    @Test
    @DisplayName("Kuaför ID'ye göre bulma - Pasif kuaför filtrelenir")
    void findById_WhenExistsButInactive_ShouldReturnEmpty() {
        // Given: Pasif tenant
        existingTenant.setActive(false);
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(existingTenant));

        // When: Service çağrılır
        Optional<TenantDto> result = tenantService.findById(1L);

        // Then: Pasif kuaför filtrelenir
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Aktif kuaförler listeleme")
    void findAllActive_ShouldReturnOnlyActiveTenants() {
        // Given: Aktif tenant'lar
        Tenant tenant1 = TestDataBuilder.createTestTenant("Kuaför 1", "+905321111111");
        Tenant tenant2 = TestDataBuilder.createTestTenant("Kuaför 2", "+905322222222");
        when(tenantRepository.findByActiveTrue())
                .thenReturn(Arrays.asList(tenant1, tenant2));

        // When: Service çağrılır
        List<TenantDto> result = tenantService.findAllActive();

        // Then: Doğru sayıda DTO döner
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TenantDto::getName)
                .containsExactlyInAnyOrder("Kuaför 1", "Kuaför 2");

        verify(tenantRepository).findByActiveTrue();
    }

    @Test
    @DisplayName("Yeni kuaför oluşturma - Başarılı senaryo")
    void createTenant_WithValidData_ShouldCreateAndReturnDto() {
        // Given: Telefon numarası müsait
        when(tenantRepository.findByPhoneNumber(tenantDto.getPhoneNumber()))
                .thenReturn(Optional.empty());

        // Repository save işlemi mock'la
        Tenant savedTenant = new Tenant();
        savedTenant.setId(1L);
        savedTenant.setName(tenantDto.getName());
        savedTenant.setPhoneNumber(tenantDto.getPhoneNumber());
        savedTenant.setActive(true);

        when(tenantRepository.save(any(Tenant.class)))
                .thenReturn(savedTenant);

        // When: Service çağrılır
        TenantDto result = tenantService.createTenant(tenantDto);

        // Then: DTO döner ve entity özellikleri doğru set edilir
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo(tenantDto.getName());
        assertThat(result.getActive()).isTrue();

        // Repository'nin doğru çağrıldığını doğrula
        verify(tenantRepository).findByPhoneNumber(tenantDto.getPhoneNumber());
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Yeni kuaför oluşturma - Telefon numarası tekrar hatası")
    void createTenant_WithDuplicatePhoneNumber_ShouldThrowException() {
        // Given: Telefon numarası zaten kayıtlı
        when(tenantRepository.findByPhoneNumber(tenantDto.getPhoneNumber()))
                .thenReturn(Optional.of(existingTenant));

        // When & Then: Exception fırlatılır
        assertThatThrownBy(() -> tenantService.createTenant(tenantDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telefon numarası zaten kullanılıyor");

        // Save çağrılmamalı
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Kuaför güncelleme - Başarılı senaryo")
    void updateTenant_WithValidData_ShouldUpdateAndReturnDto() {
        // Given: Mevcut kuaför var
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(existingTenant));

        // Telefon ve email tekrar yok
        when(tenantRepository.existsByPhoneNumberAndIdNot(anyString(), any(Long.class)))
                .thenReturn(false);
        when(tenantRepository.existsByEmailAndIdNot(anyString(), any(Long.class)))
                .thenReturn(false);

        // Save mock'u
        Tenant updatedTenant = new Tenant();
        updatedTenant.setId(1L);
        updatedTenant.setName(tenantDto.getName());
        updatedTenant.setPhoneNumber(tenantDto.getPhoneNumber());
        updatedTenant.setActive(true);

        when(tenantRepository.save(any(Tenant.class)))
                .thenReturn(updatedTenant);

        // When: Service çağrılır
        TenantDto result = tenantService.updateTenant(1L, tenantDto);

        // Then: Güncellenmiş DTO döner
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(tenantDto.getName());
        assertThat(result.getPhoneNumber()).isEqualTo(tenantDto.getPhoneNumber());

        verify(tenantRepository).findById(1L);
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Kuaför güncelleme - Bulunamayan kuaför")
    void updateTenant_WhenNotExists_ShouldThrowException() {
        // Given: Kuaför bulunamaz
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.empty());

        // When & Then: Exception fırlatılır
        assertThatThrownBy(() -> tenantService.updateTenant(1L, tenantDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kuaför bulunamadı");

        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Kuaför güncelleme - Telefon numarası başka kuaför tarafından kullanılıyor")
    void updateTenant_WithDuplicatePhoneNumber_ShouldThrowException() {
        // Given: Mevcut kuaför var ama telefon numarası başkasında
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(existingTenant));
        when(tenantRepository.existsByPhoneNumberAndIdNot(tenantDto.getPhoneNumber(), 1L))
                .thenReturn(true);

        // When & Then: Exception fırlatılır
        assertThatThrownBy(() -> tenantService.updateTenant(1L, tenantDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("telefon numarası başka bir kuaför tarafından kullanılıyor");

        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Kuaför deaktivasyon")
    void deactivateTenant_WhenExists_ShouldSetInactive() {
        // Given: Mevcut kuaför
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(existingTenant));

        // When: Deaktivasyon
        tenantService.deactivateTenant(1L);

        // Then: Active false set edilir ve kaydedilir
        verify(tenantRepository).findById(1L);
        verify(tenantRepository).save(any(Tenant.class));
        
        // Captured argument'i kontrol etmek için
        assertThat(existingTenant.getActive()).isFalse();
    }

    @Test
    @DisplayName("Kuaför aktivasyon")
    void activateTenant_WhenExists_ShouldSetActive() {
        // Given: Pasif kuaför
        existingTenant.setActive(false);
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(existingTenant));

        // When: Aktivasyon
        tenantService.activateTenant(1L);

        // Then: Active true set edilir ve kaydedilir
        verify(tenantRepository).findById(1L);
        verify(tenantRepository).save(any(Tenant.class));
        
        assertThat(existingTenant.getActive()).isTrue();
    }

    @Test
    @DisplayName("Aktif kuaför sayısı")
    void countActiveTenants_ShouldReturnRepositoryCount() {
        // Given: Repository'den sayı döner
        when(tenantRepository.countActiveTenants())
                .thenReturn(5L);

        // When: Service çağrılır
        long result = tenantService.countActiveTenants();

        // Then: Doğru sayı döner
        assertThat(result).isEqualTo(5L);

        verify(tenantRepository).countActiveTenants();
    }

    @Test
    @DisplayName("WhatsApp numarasına göre tenant ID bulma - Başarılı")
    void findTenantIdByWhatsAppNumber_WhenExists_ShouldReturnId() {
        // Given: Aktif kuaför var
        existingTenant.setActive(true);
        when(tenantRepository.findByPhoneNumber("+905321234567"))
                .thenReturn(Optional.of(existingTenant));

        // When: Service çağrılır
        Long result = tenantService.findTenantIdByWhatsAppNumber("+905321234567");

        // Then: Tenant ID döner
        assertThat(result).isEqualTo(existingTenant.getId());

        verify(tenantRepository).findByPhoneNumber("+905321234567");
    }

    @Test
    @DisplayName("WhatsApp numarasına göre tenant ID bulma - Pasif kuaför")
    void findTenantIdByWhatsAppNumber_WhenInactive_ShouldReturnNull() {
        // Given: Pasif kuaför
        existingTenant.setActive(false);
        when(tenantRepository.findByPhoneNumber("+905321234567"))
                .thenReturn(Optional.of(existingTenant));

        // When: Service çağrılır
        Long result = tenantService.findTenantIdByWhatsAppNumber("+905321234567");

        // Then: Null döner (pasif kuaför WhatsApp'ta çalışmaz)
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Kuaför adına göre arama")
    void searchByName_ShouldCallRepositoryWithCorrectParameter() {
        // Given: Repository'den sonuç döner
        List<Tenant> tenants = Arrays.asList(existingTenant);
        when(tenantRepository.findByNameContainingIgnoreCaseAndActiveTrue("test"))
                .thenReturn(tenants);

        // When: Service çağrılır
        List<TenantDto> result = tenantService.searchByName("test");

        // Then: DTO listesi döner
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo(existingTenant.getName());

        verify(tenantRepository).findByNameContainingIgnoreCaseAndActiveTrue("test");
    }

    @Test
    @DisplayName("Entity to DTO conversion test")
    void convertToDto_ShouldMapAllFields() {
        // Given: Tam dolu entity
        existingTenant.setId(1L);
        existingTenant.setName("Test Kuaför");
        existingTenant.setPhoneNumber("+905321234567");
        existingTenant.setAddress("Test Adres");
        existingTenant.setEmail("test@kuafor.com");
        existingTenant.setActive(true);

        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(existingTenant));

        // When: Service çağrılır
        Optional<TenantDto> result = tenantService.findById(1L);

        // Then: Tüm alanlar doğru map edilir
        assertThat(result).isPresent();
        TenantDto dto = result.get();
        assertThat(dto.getId()).isEqualTo(existingTenant.getId());
        assertThat(dto.getName()).isEqualTo(existingTenant.getName());
        assertThat(dto.getPhoneNumber()).isEqualTo(existingTenant.getPhoneNumber());
        assertThat(dto.getAddress()).isEqualTo(existingTenant.getAddress());
        assertThat(dto.getEmail()).isEqualTo(existingTenant.getEmail());
        assertThat(dto.getActive()).isEqualTo(existingTenant.getActive());
    }

    @Test
    @DisplayName("DTO to Entity conversion test")
    void convertToEntity_ShouldMapAllFields() {
        // Given: Tam dolu DTO
        tenantDto.setName("Test Kuaför");
        tenantDto.setPhoneNumber("+905321234567");
        tenantDto.setAddress("Test Adres");
        tenantDto.setTimezone("Europe/Istanbul");
        tenantDto.setEmail("test@kuafor.com");

        when(tenantRepository.findByPhoneNumber(tenantDto.getPhoneNumber()))
                .thenReturn(Optional.empty());

        // Entity capture için ArgumentCaptor kullan
        when(tenantRepository.save(any(Tenant.class)))
                .thenAnswer(invocation -> {
                    Tenant savedTenant = invocation.getArgument(0);
                    savedTenant.setId(1L);
                    return savedTenant;
                });

        // When: Service çağrılır
        TenantDto result = tenantService.createTenant(tenantDto);

        // Then: Entity doğru oluşturulur
        verify(tenantRepository).save(any(Tenant.class));
        assertThat(result.getName()).isEqualTo(tenantDto.getName());
        assertThat(result.getPhoneNumber()).isEqualTo(tenantDto.getPhoneNumber());
    }
}
