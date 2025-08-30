package com.example.barber.automation.service;

import com.example.barber.automation.TestDataBuilder;
import com.example.barber.automation.dto.AppointmentDto;
import com.example.barber.automation.dto.CreateAppointmentRequest;
import com.example.barber.automation.entity.*;
import com.example.barber.automation.entity.Service;
import com.example.barber.automation.repository.AppointmentRepository;
import com.example.barber.automation.repository.CustomerRepository;
import com.example.barber.automation.repository.ServiceRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AppointmentService Unit Test
 * 
 * Bu test sınıfı AppointmentService'in business logic'ini test eder:
 * - Randevu oluşturma ve validation kuralları
 * - Slot müsaitlik kontrolü entegrasyonu
 * - Randevu durumu değişiklikleri (pending -> confirmed -> completed)
 * - Randevu güncelleme ve iptal işlemleri
 * - WhatsApp'tan gelen randevu istekleri
 * - Multi-tenant randevu yönetimi
 * - Randevu çakışma kontrolü
 * - Randevu istatistikleri
 * 
 * AppointmentService sistemin core business logic'ini içerir ve
 * WhatsApp bot ile panel entegrasyonu için kritik öneme sahiptir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService Unit Tests")
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private ServiceRepository serviceRepository;
    
    @Mock
    private TenantRepository tenantRepository;
    
    @Mock
    private SlotService slotService;
    
    @Mock
    private CustomerService customerService;

    @InjectMocks
    private AppointmentService appointmentService;

    // Test data
    private Tenant testTenant;
    private Customer testCustomer;
    private Service testService;
    private Appointment testAppointment;
    private CreateAppointmentRequest appointmentRequest;

    @BeforeEach
    void setUp() {
        // Test entities
        testTenant = TestDataBuilder.createDefaultTestTenant();
        testTenant.setId(1L);

        testCustomer = TestDataBuilder.createTestCustomer("Test Customer", "+905331234567", testTenant);
        testCustomer.setId(1L);

        testService = TestDataBuilder.createDefaultHairCutService(testTenant);
        testService.setId(1L);

        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        testAppointment = TestDataBuilder.createTestAppointment(appointmentTime, testCustomer, testService, testTenant);
        testAppointment.setId(1L);

        // Test request
        appointmentRequest = new CreateAppointmentRequest();
        appointmentRequest.setCustomerId(1L);
        appointmentRequest.setServiceId(1L);
        appointmentRequest.setStartTime(appointmentTime);
        appointmentRequest.setNotes("Test randevu notu");
    }

    @Test
    @DisplayName("Randevu ID'ye göre bulma - Tenant isolasyonu")
    void findById_WithTenantIsolation_ShouldReturnAppointment() {
        // Given: Belirli tenant'a ait randevu
        when(appointmentRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(testAppointment));

        // When: Service çağrılır
        Optional<AppointmentDto> result = appointmentService.findById(1L, 1L);

        // Then: Doğru randevu döner
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getTenantId()).isEqualTo(1L);

        verify(appointmentRepository).findByIdAndTenantId(1L, 1L);
    }

    @Test
    @DisplayName("Aktif randevuları listeleme")
    void findActiveAppointments_ShouldReturnActiveDtos() {
        // Given: Aktif randevular
        Appointment appointment1 = TestDataBuilder.createTestAppointment(
                LocalDateTime.now().plusHours(1), testCustomer, testService, testTenant);
        appointment1.setStatus(Appointment.AppointmentStatus.CONFIRMED);
        
        Appointment appointment2 = TestDataBuilder.createTestAppointment(
                LocalDateTime.now().plusHours(2), testCustomer, testService, testTenant);
        appointment2.setStatus(Appointment.AppointmentStatus.PENDING);

        when(appointmentRepository.findActiveAppointmentsByTenantId(1L))
                .thenReturn(Arrays.asList(appointment1, appointment2));

        // When: Service çağrılır
        List<AppointmentDto> result = appointmentService.findActiveAppointments(1L);

        // Then: Aktif randevular döner
        assertThat(result).hasSize(2);
        assertThat(result).extracting(AppointmentDto::getStatus)
                .containsExactlyInAnyOrder(
                        Appointment.AppointmentStatus.CONFIRMED,
                        Appointment.AppointmentStatus.PENDING
                );

        verify(appointmentRepository).findActiveAppointmentsByTenantId(1L);
    }

    @Test
    @DisplayName("Tarih aralığına göre randevuları getirme")
    void findByDateRange_ShouldReturnAppointmentsInRange() {
        // Given: Belirli tarih aralığı
        LocalDateTime startDate = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endDate = LocalDateTime.now().toLocalDate().atTime(23, 59, 59);

        when(appointmentRepository.findByTenantIdAndDateRange(1L, startDate, endDate))
                .thenReturn(Arrays.asList(testAppointment));

        // When: Service çağrılır
        List<AppointmentDto> result = appointmentService.findByDateRange(1L, startDate, endDate);

        // Then: Tarih aralığındaki randevular döner
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(testAppointment.getId());

        verify(appointmentRepository).findByTenantIdAndDateRange(1L, startDate, endDate);
    }

    @Test
    @DisplayName("Bugünkü randevuları getirme")
    void findTodayAppointments_ShouldReturnTodayAppointments() {
        // Given: Bugünkü randevular
        when(appointmentRepository.findTodayAppointments(1L))
                .thenReturn(Arrays.asList(testAppointment));

        // When: Service çağrılır
        List<AppointmentDto> result = appointmentService.findTodayAppointments(1L);

        // Then: Bugünkü randevular döner
        assertThat(result).hasSize(1);
        verify(appointmentRepository).findTodayAppointments(1L);
    }

    @Test
    @DisplayName("Yeni randevu oluşturma - Başarılı senaryo")
    void createAppointment_WithValidData_ShouldCreateAppointment() {
        // Given: Valid data ve müsait slot
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(testTenant));
        when(customerRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(testCustomer));
        when(serviceRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(testService));
        when(slotService.isSlotAvailable(1L, 1L, appointmentRequest.getStartTime()))
                .thenReturn(true);

        Appointment savedAppointment = TestDataBuilder.createTestAppointment(
                appointmentRequest.getStartTime(), testCustomer, testService, testTenant);
        savedAppointment.setId(1L);
        savedAppointment.setNotes(appointmentRequest.getNotes());

        when(appointmentRepository.save(any(Appointment.class)))
                .thenReturn(savedAppointment);

        // When: Service çağrılır
        AppointmentDto result = appointmentService.createAppointment(appointmentRequest, 1L);

        // Then: Randevu oluşturulur
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(Appointment.AppointmentStatus.PENDING);
        assertThat(result.getNotes()).isEqualTo(appointmentRequest.getNotes());

        verify(slotService).isSlotAvailable(1L, 1L, appointmentRequest.getStartTime());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Yeni randevu oluşturma - Geçersiz tenant")
    void createAppointment_WithInvalidTenant_ShouldThrowException() {
        // Given: Geçersiz tenant
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.empty());

        // When & Then: Exception fırlatılır
        assertThatThrownBy(() -> appointmentService.createAppointment(appointmentRequest, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kuaför bulunamadı");

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Yeni randevu oluşturma - Müsait olmayan slot")
    void createAppointment_WithUnavailableSlot_ShouldThrowException() {
        // Given: Geçerli data ama müsait olmayan slot
        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(testTenant));
        when(customerRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(testCustomer));
        when(serviceRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(testService));
        when(slotService.isSlotAvailable(1L, 1L, appointmentRequest.getStartTime()))
                .thenReturn(false);

        // When & Then: Exception fırlatılır
        assertThatThrownBy(() -> appointmentService.createAppointment(appointmentRequest, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Seçilen saat artık müsait değil");

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("WhatsApp'tan randevu oluşturma - Otomatik müşteri oluşturma")
    void createAppointment_FromWhatsApp_ShouldCreateCustomerAutomatically() {
        // Given: WhatsApp'tan gelen request (customer ID yok, telefon var)
        appointmentRequest.setCustomerId(null);
        appointmentRequest.setCustomerName("WhatsApp User");
        appointmentRequest.setCustomerPhone("+905331234567");

        when(tenantRepository.findById(1L))
                .thenReturn(Optional.of(testTenant));
        when(serviceRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(testService));
        when(slotService.isSlotAvailable(1L, 1L, appointmentRequest.getStartTime()))
                .thenReturn(true);

        // CustomerService mock'u
        when(customerService.createCustomerFromWhatsApp("WhatsApp User", "+905331234567", 1L))
                .thenReturn(convertToDto(testCustomer));
        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(testCustomer));

        Appointment savedAppointment = TestDataBuilder.createTestAppointment(
                appointmentRequest.getStartTime(), testCustomer, testService, testTenant);
        savedAppointment.setId(1L);

        when(appointmentRepository.save(any(Appointment.class)))
                .thenReturn(savedAppointment);

        // When: Service çağrılır
        AppointmentDto result = appointmentService.createAppointment(appointmentRequest, 1L);

        // Then: Randevu oluşturulur ve müşteri otomatik oluşturulur
        assertThat(result).isNotNull();
        verify(customerService).createCustomerFromWhatsApp("WhatsApp User", "+905331234567", 1L);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Randevu onaylama - Pending'den Confirmed'e")
    void confirmAppointment_WhenPending_ShouldSetConfirmed() {
        // Given: Pending randevu
        testAppointment.setStatus(Appointment.AppointmentStatus.PENDING);
        when(appointmentRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(testAppointment));

        Appointment confirmedAppointment = TestDataBuilder.createTestAppointment(
                testAppointment.getStartTime(), testCustomer, testService, testTenant);
        confirmedAppointment.setId(1L);
        confirmedAppointment.setStatus(Appointment.AppointmentStatus.CONFIRMED);

        when(appointmentRepository.save(any(Appointment.class)))
                .thenReturn(confirmedAppointment);

        // When: Service çağrılır
        AppointmentDto result = appointmentService.confirmAppointment(1L, 1L);

        // Then: Status confirmed olur
        assertThat(result.getStatus()).isEqualTo(Appointment.AppointmentStatus.CONFIRMED);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Randevu onaylama - Pending olmayan randevu")
    void confirmAppointment_WhenNotPending_ShouldThrowException() {
        // Given: Confirmed randevu
        testAppointment.setStatus(Appointment.AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(testAppointment));

        // When & Then: Exception fırlatılır
        assertThatThrownBy(() -> appointmentService.confirmAppointment(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sadece beklemedeki randevular onaylanabilir");

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Randevu tamamlama - Completed status")
    void completeAppointment_WhenNotCancelled_ShouldSetCompleted() {
        // Given: Aktif randevu
        testAppointment.setStatus(Appointment.AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(testAppointment));

        Appointment completedAppointment = TestDataBuilder.createTestAppointment(
                testAppointment.getStartTime(), testCustomer, testService, testTenant);
        completedAppointment.setId(1L);
        completedAppointment.setStatus(Appointment.AppointmentStatus.COMPLETED);

        when(appointmentRepository.save(any(Appointment.class)))
                .thenReturn(completedAppointment);

        // When: Service çağrılır
        AppointmentDto result = appointmentService.completeAppointment(1L, 1L);

        // Then: Status completed olur
        assertThat(result.getStatus()).isEqualTo(Appointment.AppointmentStatus.COMPLETED);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Randevu iptal etme")
    void cancelAppointment_WithReason_ShouldSetCancelledAndAddNote() {
        // Given: Aktif randevu
        testAppointment.setStatus(Appointment.AppointmentStatus.PENDING);
        testAppointment.setNotes("Önceki not");
        when(appointmentRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(testAppointment));

        Appointment cancelledAppointment = TestDataBuilder.createTestAppointment(
                testAppointment.getStartTime(), testCustomer, testService, testTenant);
        cancelledAppointment.setId(1L);
        cancelledAppointment.setStatus(Appointment.AppointmentStatus.CANCELLED);

        when(appointmentRepository.save(any(Appointment.class)))
                .thenReturn(cancelledAppointment);

        // When: Service çağrılır
        String cancelReason = "Müşteri iptal etti";
        AppointmentDto result = appointmentService.cancelAppointment(1L, 1L, cancelReason);

        // Then: Status cancelled olur ve not eklenir
        assertThat(result.getStatus()).isEqualTo(Appointment.AppointmentStatus.CANCELLED);
        
        // Not güncellenmesi kontrol edilir
        assertThat(testAppointment.getNotes()).contains(cancelReason);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Randevu güncelleme - Yeni zaman müsait")
    void updateAppointment_WithNewTime_ShouldUpdateWhenAvailable() {
        // Given: Güncellenecek randevu ve yeni zaman
        testAppointment.setStatus(Appointment.AppointmentStatus.PENDING);
        when(appointmentRepository.findByIdAndTenantId(1L, 1L))
                .thenReturn(Optional.of(testAppointment));

        LocalDateTime newTime = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        appointmentRequest.setStartTime(newTime);

        // Çakışma kontrolü - çakışan randevu yok (kendisi hariç)
        when(appointmentRepository.findConflictingAppointments(eq(1L), eq(newTime), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList()); // Boş liste - çakışma yok

        Appointment updatedAppointment = TestDataBuilder.createTestAppointment(
                newTime, testCustomer, testService, testTenant);
        updatedAppointment.setId(1L);

        when(appointmentRepository.save(any(Appointment.class)))
                .thenReturn(updatedAppointment);

        // When: Service çağrılır
        AppointmentDto result = appointmentService.updateAppointment(1L, appointmentRequest, 1L);

        // Then: Randevu güncellenir
        assertThat(result.getStartTime()).isEqualTo(newTime);
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Müşterinin aktif randevu kontrolü")
    void hasActiveAppointment_ShouldReturnRepositoryResult() {
        // Given: Repository'den sonuç döner
        when(appointmentRepository.hasActiveAppointment(1L, 1L))
                .thenReturn(true);

        // When: Service çağrılır
        boolean result = appointmentService.hasActiveAppointment(1L, 1L);

        // Then: Repository sonucu döner
        assertThat(result).isTrue();
        verify(appointmentRepository).hasActiveAppointment(1L, 1L);
    }

    @Test
    @DisplayName("Randevu istatistikleri")
    void getAppointmentStats_ShouldReturnAllStatuses() {
        // Given: Repository'den status sayıları döner
        when(appointmentRepository.countByTenantIdAndStatus(1L, Appointment.AppointmentStatus.PENDING))
                .thenReturn(5L);
        when(appointmentRepository.countByTenantIdAndStatus(1L, Appointment.AppointmentStatus.CONFIRMED))
                .thenReturn(10L);
        when(appointmentRepository.countByTenantIdAndStatus(1L, Appointment.AppointmentStatus.COMPLETED))
                .thenReturn(20L);
        when(appointmentRepository.countByTenantIdAndStatus(1L, Appointment.AppointmentStatus.CANCELLED))
                .thenReturn(3L);

        // When: Service çağrılır
        Map<String, Long> result = appointmentService.getAppointmentStats(1L);

        // Then: Tüm status'ler için sayılar döner
        assertThat(result).hasSize(4);
        assertThat(result.get("pending")).isEqualTo(5L);
        assertThat(result.get("confirmed")).isEqualTo(10L);
        assertThat(result.get("completed")).isEqualTo(20L);
        assertThat(result.get("cancelled")).isEqualTo(3L);

        // Tüm status'ler için repository çağrıldı mı?
        verify(appointmentRepository, times(4)).countByTenantIdAndStatus(eq(1L), any(Appointment.AppointmentStatus.class));
    }

    @Test
    @DisplayName("WhatsApp randevu onay mesajı formatı")
    void getAppointmentConfirmationMessage_ShouldReturnFormattedMessage() {
        // Given: Appointment DTO
        AppointmentDto appointmentDto = convertToDtoWithDetails(testAppointment);

        // When: Service çağrılır
        String result = appointmentService.getAppointmentConfirmationMessage(appointmentDto);

        // Then: Formatted mesaj döner
        assertThat(result).contains("Randevunuz Onaylandı");
        assertThat(result).contains(testCustomer.getName());
        assertThat(result).contains(testService.getName());
        assertThat(result).contains("📅");
        assertThat(result).contains("⏰");
    }

    @Test
    @DisplayName("Yaklaşan randevuları getirme")
    void findUpcomingAppointments_ShouldReturnNext7Days() {
        // Given: Yaklaşan randevular
        when(appointmentRepository.findUpcomingAppointments(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(testAppointment));

        // When: Service çağrılır
        List<AppointmentDto> result = appointmentService.findUpcomingAppointments(1L);

        // Then: Yaklaşan randevular döner
        assertThat(result).hasSize(1);
        
        // Repository'nin doğru tarih aralığıyla çağrıldığını doğrula
        verify(appointmentRepository).findUpcomingAppointments(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Müşteriye ait randevuları getirme")
    void findByCustomer_ShouldReturnCustomerAppointments() {
        // Given: Müşteriye ait randevular
        when(appointmentRepository.findByTenantIdAndCustomerId(1L, 1L))
                .thenReturn(Arrays.asList(testAppointment));

        // When: Service çağrılır
        List<AppointmentDto> result = appointmentService.findByCustomer(1L, 1L);

        // Then: Müşteri randevuları döner
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo(1L);

        verify(appointmentRepository).findByTenantIdAndCustomerId(1L, 1L);
    }

    // Helper methods
    private com.example.barber.automation.dto.CustomerDto convertToDto(Customer customer) {
        com.example.barber.automation.dto.CustomerDto dto = new com.example.barber.automation.dto.CustomerDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setPhoneNumber(customer.getPhoneNumber());
        return dto;
    }

    private AppointmentDto convertToDtoWithDetails(Appointment appointment) {
        AppointmentDto dto = new AppointmentDto();
        dto.setId(appointment.getId());
        dto.setStartTime(appointment.getStartTime());
        dto.setEndTime(appointment.getEndTime());
        dto.setStatus(appointment.getStatus());
        dto.setTotalPrice(appointment.getTotalPrice());
        dto.setCurrency(appointment.getCurrency());

        // Customer DTO
        com.example.barber.automation.dto.CustomerDto customerDto = new com.example.barber.automation.dto.CustomerDto();
        customerDto.setId(appointment.getCustomer().getId());
        customerDto.setName(appointment.getCustomer().getName());
        dto.setCustomer(customerDto);

        // Service DTO
        com.example.barber.automation.dto.ServiceDto serviceDto = new com.example.barber.automation.dto.ServiceDto();
        serviceDto.setId(appointment.getService().getId());
        serviceDto.setName(appointment.getService().getName());
        serviceDto.setPrice(appointment.getService().getPrice());
        serviceDto.setCurrency(appointment.getService().getCurrency());
        dto.setService(serviceDto);

        return dto;
    }
}
