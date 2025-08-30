package com.example.barber.automation.controller;

import com.example.barber.automation.dto.AppointmentDto;
import com.example.barber.automation.dto.CreateAppointmentRequest;
import com.example.barber.automation.dto.CustomerDto;
import com.example.barber.automation.dto.ServiceDto;
import com.example.barber.automation.entity.Appointment;
import com.example.barber.automation.service.AppointmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Simple AppointmentController REST API Test
 * 
 * Bu test sınıfı AppointmentController'ın gerçek HTTP endpoint'lerini test eder.
 * Security configuration olmadan sadece HTTP layer test edilir.
 */
@WebMvcTest(controllers = AppointmentController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@DisplayName("Simple AppointmentController REST API Tests")
class SimpleAppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppointmentService appointmentService;

    // Test data
    private AppointmentDto appointmentDto;
    private CreateAppointmentRequest createRequest;
    private CustomerDto customerDto;
    private ServiceDto serviceDto;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        // Customer DTO
        customerDto = new CustomerDto();
        customerDto.setId(1L);
        customerDto.setName("Test Müşteri");
        customerDto.setPhoneNumber("+905331234567");

        // Service DTO
        serviceDto = new ServiceDto();
        serviceDto.setId(1L);
        serviceDto.setName("Saç Kesimi");
        serviceDto.setDurationMinutes(45);
        serviceDto.setPrice(new BigDecimal("150.00"));
        serviceDto.setCurrency("TRY");

        // Appointment DTO
        appointmentDto = new AppointmentDto();
        appointmentDto.setId(1L);
        appointmentDto.setStartTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
        appointmentDto.setEndTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(45));
        appointmentDto.setStatus(Appointment.AppointmentStatus.PENDING);
        appointmentDto.setCustomer(customerDto);
        appointmentDto.setService(serviceDto);
        appointmentDto.setTotalPrice(new BigDecimal("150.00"));
        appointmentDto.setCurrency("TRY");
        appointmentDto.setNotes("Test randevu notu");

        // Create appointment request
        createRequest = new CreateAppointmentRequest();
        createRequest.setCustomerId(1L);
        createRequest.setServiceId(1L);
        createRequest.setStartTime(appointmentDto.getStartTime());
        createRequest.setNotes("Test randevu notu");
    }

    @Test
    @DisplayName("GET /tenants/{tenantId}/appointments/active - Aktif randevu listesi")
    void getActiveAppointments_ShouldReturnAppointmentList() throws Exception {
        // Given: Service'den randevu listesi döner
        List<AppointmentDto> appointments = Arrays.asList(appointmentDto);
        when(appointmentService.findActiveAppointments(TENANT_ID)).thenReturn(appointments);

        // When & Then: GET request
        mockMvc.perform(get("/tenants/{tenantId}/appointments/active", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")))
                .andExpect(jsonPath("$[0].customer.name", is("Test Müşteri")))
                .andExpect(jsonPath("$[0].service.name", is("Saç Kesimi")));

        verify(appointmentService).findActiveAppointments(TENANT_ID);
    }

    @Test
    @DisplayName("GET /tenants/{tenantId}/appointments/{id} - Randevu ID ile getirme")
    void getAppointmentById_WhenExists_ShouldReturnAppointment() throws Exception {
        // Given: Service'den randevu döner
        when(appointmentService.findById(1L, TENANT_ID)).thenReturn(Optional.of(appointmentDto));

        // When & Then: GET request
        mockMvc.perform(get("/tenants/{tenantId}/appointments/{id}", TENANT_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.notes", is("Test randevu notu")));

        verify(appointmentService).findById(1L, TENANT_ID);
    }

    @Test
    @DisplayName("GET /tenants/{tenantId}/appointments/{id} - Bulunamayan randevu için 404")
    void getAppointmentById_WhenNotExists_ShouldReturn404() throws Exception {
        // Given: Service'den empty döner
        when(appointmentService.findById(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When & Then: GET request
        mockMvc.perform(get("/tenants/{tenantId}/appointments/{id}", TENANT_ID, 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(appointmentService).findById(999L, TENANT_ID);
    }

    @Test
    @DisplayName("POST /tenants/{tenantId}/appointments - Yeni randevu oluşturma")
    void createAppointment_WithValidData_ShouldReturn201() throws Exception {
        // Given: Service'den yeni randevu döner
        when(appointmentService.createAppointment(any(CreateAppointmentRequest.class), eq(TENANT_ID)))
                .thenReturn(appointmentDto);

        // When & Then: POST request
        mockMvc.perform(post("/tenants/{tenantId}/appointments", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("PENDING")));

        verify(appointmentService).createAppointment(any(CreateAppointmentRequest.class), eq(TENANT_ID));
    }

    @Test
    @DisplayName("PUT /tenants/{tenantId}/appointments/{id}/confirm - Randevu onaylama")
    void confirmAppointment_WhenPending_ShouldReturn200() throws Exception {
        // Given: Service'den onaylanmış randevu döner
        appointmentDto.setStatus(Appointment.AppointmentStatus.CONFIRMED);
        when(appointmentService.confirmAppointment(1L, TENANT_ID)).thenReturn(appointmentDto);

        // When & Then: PUT request
        mockMvc.perform(post("/tenants/{tenantId}/appointments/{id}/confirm", TENANT_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        verify(appointmentService).confirmAppointment(1L, TENANT_ID);
    }

    @Test
    @DisplayName("PUT /tenants/{tenantId}/appointments/{id}/cancel - Randevu iptal etme")
    void cancelAppointment_WithReason_ShouldReturn200() throws Exception {
        // Given: Service'den iptal edilmiş randevu döner
        appointmentDto.setStatus(Appointment.AppointmentStatus.CANCELLED);
        when(appointmentService.cancelAppointment(eq(1L), eq(TENANT_ID), anyString())).thenReturn(appointmentDto);

        Map<String, String> cancelRequest = new HashMap<>();
        cancelRequest.put("reason", "Müşteri iptal etti");

        // When & Then: POST request
        mockMvc.perform(post("/tenants/{tenantId}/appointments/{id}/cancel", TENANT_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(appointmentService).cancelAppointment(eq(1L), eq(TENANT_ID), eq("Müşteri iptal etti"));
    }

    @Test
    @DisplayName("PUT /tenants/{tenantId}/appointments/{id}/complete - Randevu tamamlama")
    void completeAppointment_WhenActive_ShouldReturn200() throws Exception {
        // Given: Service'den tamamlanmış randevu döner
        appointmentDto.setStatus(Appointment.AppointmentStatus.COMPLETED);
        when(appointmentService.completeAppointment(1L, TENANT_ID)).thenReturn(appointmentDto);

        // When & Then: PUT request
        mockMvc.perform(post("/tenants/{tenantId}/appointments/{id}/complete", TENANT_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        verify(appointmentService).completeAppointment(1L, TENANT_ID);
    }

    @Test
    @DisplayName("GET /tenants/{tenantId}/appointments/today - Bugünkü randevular")
    void getTodayAppointments_ShouldReturnTodayList() throws Exception {
        // Given: Service'den bugünkü randevular döner
        List<AppointmentDto> todayAppointments = Arrays.asList(appointmentDto);
        when(appointmentService.findTodayAppointments(TENANT_ID)).thenReturn(todayAppointments);

        // When & Then: GET request
        mockMvc.perform(get("/tenants/{tenantId}/appointments/today", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)));

        verify(appointmentService).findTodayAppointments(TENANT_ID);
    }

    @Test
    @DisplayName("GET /tenants/{tenantId}/appointments/stats - Randevu istatistikleri")
    void getAppointmentStats_ShouldReturnStatistics() throws Exception {
        // Given: Service'den istatistikler döner
        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", 5L);
        stats.put("confirmed", 10L);
        stats.put("completed", 20L);
        stats.put("cancelled", 3L);

        when(appointmentService.getAppointmentStats(TENANT_ID)).thenReturn(stats);

        // When & Then: GET request
        mockMvc.perform(get("/tenants/{tenantId}/appointments/stats", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.pending", is(5)))
                .andExpect(jsonPath("$.confirmed", is(10)))
                .andExpect(jsonPath("$.completed", is(20)))
                .andExpect(jsonPath("$.cancelled", is(3)));

        verify(appointmentService).getAppointmentStats(TENANT_ID);
    }
}
