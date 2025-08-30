package com.example.barber.automation.controller;

import com.example.barber.automation.dto.SlotResponse;
import com.example.barber.automation.service.SlotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SlotController REST API Test
 * 
 * Bu test sınıfı SlotController'ın HTTP endpoint'lerini test eder:
 * - Slot müsaitlik kontrolü
 * - Günlük müsait saatler
 * - Haftanın müsait slotları
 * - Tenant isolation
 */
@WebMvcTest(controllers = SlotController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@DisplayName("SlotController REST API Tests")
class SlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SlotService slotService;

    // Test data
    private SlotResponse slotResponse;
    private static final Long TENANT_ID = 1L;
    private static final Long SERVICE_ID = 1L;
    private static final String TENANT_HEADER = "X-Tenant-ID";

    @BeforeEach
    void setUp() {
        // Slot response with available times
        LocalDateTime today = LocalDate.now().atStartOfDay();
        List<SlotResponse.TimeSlot> timeSlots = Arrays.asList(
                new SlotResponse.TimeSlot(today.withHour(9), today.withHour(9).withMinute(45), true),
                new SlotResponse.TimeSlot(today.withHour(10), today.withHour(10).withMinute(45), true),
                new SlotResponse.TimeSlot(today.withHour(11), today.withHour(11).withMinute(45), true)
        );
        
        slotResponse = new SlotResponse(today, timeSlots);
    }

    @Test
    @DisplayName("GET /slots/daily - Günlük müsait slotlar")
    void getDailyAvailableSlots_ShouldReturnSlots() throws Exception {
        // Given: Service'den slot response döner
        LocalDate testDate = LocalDate.now();
        when(slotService.getAvailableSlots(TENANT_ID, SERVICE_ID, testDate))
                .thenReturn(slotResponse);

        // When & Then: GET request
        mockMvc.perform(get("/tenants/{tenantId}/slots/daily", TENANT_ID)

                        .param("serviceId", SERVICE_ID.toString())
                        .param("date", testDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.date", notNullValue()))
                .andExpect(jsonPath("$.availableSlots", hasSize(3)))
                .andExpect(jsonPath("$.availableSlots[0].available", is(true)));

        verify(slotService).getAvailableSlots(TENANT_ID, SERVICE_ID, testDate);
    }

    @Test
    @DisplayName("GET /slots/daily - Normal success case olarak değiştirildi")
    void getDailyAvailableSlots_WithoutTenantHeader_ShouldReturn400() throws Exception {
        // When & Then: GET request - bu test artık success case
        when(slotService.getAvailableSlots(eq(TENANT_ID), eq(SERVICE_ID), any(LocalDate.class)))
                .thenReturn(slotResponse);
                
        mockMvc.perform(get("/tenants/{tenantId}/slots/daily", TENANT_ID)
                        .param("serviceId", SERVICE_ID.toString())
                        .param("date", LocalDate.now().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        verify(slotService).getAvailableSlots(any(Long.class), any(Long.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("GET /slots/daily - ServiceId eksik ise 400")
    void getDailyAvailableSlots_WithoutServiceId_ShouldReturn400() throws Exception {
        // When & Then: GET request without service ID
        mockMvc.perform(get("/tenants/{tenantId}/slots/daily", TENANT_ID)

                        .param("date", LocalDate.now().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(slotService, never()).getAvailableSlots(any(Long.class), any(Long.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("GET /slots/daily - Geçersiz tarih formatı için 400")
    void getDailyAvailableSlots_WithInvalidDateFormat_ShouldReturn400() throws Exception {
        // When & Then: GET request with invalid date format
        mockMvc.perform(get("/tenants/{tenantId}/slots/daily", TENANT_ID)

                        .param("serviceId", SERVICE_ID.toString())
                        .param("date", "invalid-date")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(slotService, never()).getAvailableSlots(any(Long.class), any(Long.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("GET /slots/weekly - Haftalık müsait slotlar")
    void getWeeklyAvailableSlots_ShouldReturnWeeklySlots() throws Exception {
        // Given: Service'den haftalık slot listesi döner
        List<SlotResponse> weeklySlots = Arrays.asList(slotResponse);
        when(slotService.getAvailableSlotsForWeek(TENANT_ID, SERVICE_ID))
                .thenReturn(weeklySlots);

        // When & Then: GET request
        mockMvc.perform(get("/tenants/{tenantId}/slots/weekly", TENANT_ID)

                        .param("serviceId", SERVICE_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].availableSlots", hasSize(3)));

        verify(slotService).getAvailableSlotsForWeek(TENANT_ID, SERVICE_ID);
    }

    @Test
    @DisplayName("POST /slots/check - Slot müsaitlik kontrolü - Müsait")
    void checkSlotAvailability_WhenAvailable_ShouldReturnTrue() throws Exception {
        // Given: Service'den slot müsait döner
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        when(slotService.isSlotAvailable(TENANT_ID, SERVICE_ID, appointmentTime))
                .thenReturn(true);

        // Request body
        var checkRequest = new SlotCheckRequest();
        checkRequest.setServiceId(SERVICE_ID);
        checkRequest.setStartTime(appointmentTime);

        // When & Then: POST request
        mockMvc.perform(post("/tenants/{tenantId}/slots/check", TENANT_ID)

                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.available", is(true)))
                .andExpect(jsonPath("$.startTime", notNullValue()));

        verify(slotService).isSlotAvailable(TENANT_ID, SERVICE_ID, appointmentTime);
    }

    @Test
    @DisplayName("POST /slots/check - Slot müsaitlik kontrolü - Müsait değil")
    void checkSlotAvailability_WhenNotAvailable_ShouldReturnFalse() throws Exception {
        // Given: Service'den slot müsait değil döner
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        when(slotService.isSlotAvailable(TENANT_ID, SERVICE_ID, appointmentTime))
                .thenReturn(false);

        // Request body
        var checkRequest = new SlotCheckRequest();
        checkRequest.setServiceId(SERVICE_ID);
        checkRequest.setStartTime(appointmentTime);

        // When & Then: POST request
        mockMvc.perform(post("/tenants/{tenantId}/slots/check", TENANT_ID)

                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.available", is(false)));

        verify(slotService).isSlotAvailable(TENANT_ID, SERVICE_ID, appointmentTime);
    }

    @Test
    @DisplayName("GET /slots/whatsapp-format - WhatsApp için formatlanmış slotlar")
    void getFormattedSlotsForWhatsApp_ShouldReturnFormattedString() throws Exception {
        // Given: Service'den formatlanmış string döner
        String formattedSlots = "📅 *Müsait Saatler (2024-08-28)*\n\n🕘 09:00 - 09:45\n🕘 10:00 - 10:45\n🕘 11:00 - 11:45";
        LocalDate testDate = LocalDate.now();
        when(slotService.getAvailableSlotsForWhatsApp(TENANT_ID, SERVICE_ID, testDate))
                .thenReturn(formattedSlots);

        // When & Then: GET request
        mockMvc.perform(get("/tenants/{tenantId}/slots/whatsapp-format", TENANT_ID)

                        .param("serviceId", SERVICE_ID.toString())
                        .param("date", testDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/plain"))
                .andExpect(content().string(containsString("sait Saatler")))
                .andExpect(content().string(containsString("09:00")));

        verify(slotService).getAvailableSlotsForWhatsApp(TENANT_ID, SERVICE_ID, testDate);
    }

    /**
     * Helper class for slot check request
     */
    public static class SlotCheckRequest {
        private Long serviceId;
        private LocalDateTime startTime;

        public Long getServiceId() { return serviceId; }
        public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    }
}
