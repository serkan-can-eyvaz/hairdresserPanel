package com.example.barber.automation.controller;

import com.example.barber.automation.dto.TenantDto;
import com.example.barber.automation.service.TenantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TenantController REST API Test
 * 
 * Bu test sınıfı TenantController'ın HTTP endpoint'lerini test eder:
 * - HTTP status kodları (200, 201, 400, 404, 409)
 * - Request/Response JSON formatları
 * - Validation error handling
 * - Path variable ve query parameter işleme
 * - RESTful API contract doğrulaması
 * 
 * @WebMvcTest sadece web layer'ı yükler (hızlı test)
 * Service katmanı mock'lanır, HTTP layer odaklı test yapılır.
 */
@WebMvcTest(controllers = TenantController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@DisplayName("TenantController REST API Tests")
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TenantService tenantService;

    // Test data
    private TenantDto tenantDto;
    private TenantDto existingTenant;

    @BeforeEach
    void setUp() {
        // Valid tenant DTO for requests
        tenantDto = new TenantDto();
        tenantDto.setName("Test Kuaför");
        tenantDto.setPhoneNumber("+905321234567");
        tenantDto.setAddress("Test Adres, İstanbul");
        tenantDto.setEmail("test@kuafor.com");
        tenantDto.setTimezone("Europe/Istanbul");

        // Existing tenant for responses
        existingTenant = new TenantDto();
        existingTenant.setId(1L);
        existingTenant.setName("Mevcut Kuaför");
        existingTenant.setPhoneNumber("+905321111111");
        existingTenant.setAddress("Mevcut Adres");
        existingTenant.setEmail("mevcut@kuafor.com");
        existingTenant.setActive(true);
    }

    @Test
    @DisplayName("GET /tenants - Aktif kuaförler listeleme")
    void getAllActiveTenants_ShouldReturnTenantList() throws Exception {
        // Given: Service'den tenant listesi döner
        List<TenantDto> tenants = Arrays.asList(existingTenant);
        when(tenantService.findAllActive()).thenReturn(tenants);

        // When & Then: GET request
        mockMvc.perform(get("/tenants")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Mevcut Kuaför")))
                .andExpect(jsonPath("$[0].active", is(true)));

        verify(tenantService).findAllActive();
    }

    @Test
    @DisplayName("GET /tenants/{id} - Tenant ID ile getirme - Başarılı")
    void getTenantById_WhenExists_ShouldReturnTenant() throws Exception {
        // Given: Service'den tenant döner
        when(tenantService.findById(1L)).thenReturn(Optional.of(existingTenant));

        // When & Then: GET request
        mockMvc.perform(get("/tenants/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Mevcut Kuaför")))
                .andExpect(jsonPath("$.phoneNumber", is("+905321111111")));

        verify(tenantService).findById(1L);
    }

    @Test
    @DisplayName("GET /tenants/{id} - Bulunamayan tenant için 404")
    void getTenantById_WhenNotExists_ShouldReturn404() throws Exception {
        // Given: Service'den empty döner
        when(tenantService.findById(999L)).thenReturn(Optional.empty());

        // When & Then: GET request
        mockMvc.perform(get("/tenants/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(tenantService).findById(999L);
    }

    @Test
    @DisplayName("POST /tenants - Yeni kuaför oluşturma - Başarılı")
    void createTenant_WithValidData_ShouldReturn201() throws Exception {
        // Given: Service'den yeni tenant döner
        TenantDto createdTenant = new TenantDto();
        createdTenant.setId(1L);
        createdTenant.setName(tenantDto.getName());
        createdTenant.setPhoneNumber(tenantDto.getPhoneNumber());
        createdTenant.setActive(true);

        when(tenantService.createTenant(any(TenantDto.class))).thenReturn(createdTenant);

        // When & Then: POST request
        mockMvc.perform(post("/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tenantDto)))
                .andDo(print())
                .andExpect(status().isCreated())

                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is(tenantDto.getName())))
                .andExpect(jsonPath("$.active", is(true)));

        verify(tenantService).createTenant(any(TenantDto.class));
    }

    @Test
    @DisplayName("POST /tenants - Geçersiz JSON formatı için 400")
    void createTenant_WithInvalidJson_ShouldReturn400() throws Exception {
        // When & Then: Invalid JSON
        mockMvc.perform(post("/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(tenantService, never()).createTenant(any(TenantDto.class));
    }

    @Test
    @DisplayName("POST /tenants - Validation hatası için 400")
    void createTenant_WithValidationErrors_ShouldReturn400() throws Exception {
        // Given: Invalid tenant data
        TenantDto invalidTenant = new TenantDto();
        invalidTenant.setName(""); // Boş isim
        invalidTenant.setPhoneNumber("invalid-phone"); // Geçersiz telefon
        invalidTenant.setEmail("invalid-email"); // Geçersiz email

        // When & Then: POST request with validation errors
        mockMvc.perform(post("/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTenant)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(tenantService, never()).createTenant(any(TenantDto.class));
    }

    @Test
    @DisplayName("POST /tenants - Telefon numarası tekrar için 409")
    void createTenant_WithDuplicatePhoneNumber_ShouldReturn409() throws Exception {
        // Given: Service'den telefon numarası tekrar hatası
        when(tenantService.createTenant(any(TenantDto.class)))
                .thenThrow(new IllegalArgumentException("Bu telefon numarası zaten kullanılıyor"));

        // When & Then: POST request
        mockMvc.perform(post("/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tenantDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(tenantService).createTenant(any(TenantDto.class));
    }

    @Test
    @DisplayName("PUT /tenants/{id} - Kuaför güncelleme - Başarılı")
    void updateTenant_WithValidData_ShouldReturn200() throws Exception {
        // Given: Service'den güncellenmiş tenant döner
        existingTenant.setName("Güncellenmiş Kuaför");
        when(tenantService.updateTenant(eq(1L), any(TenantDto.class))).thenReturn(existingTenant);

        // When & Then: PUT request
        mockMvc.perform(put("/tenants/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tenantDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("Güncellenmiş Kuaför")));

        verify(tenantService).updateTenant(eq(1L), any(TenantDto.class));
    }

    @Test
    @DisplayName("PUT /tenants/{id} - Bulunamayan tenant için 404")
    void updateTenant_WhenNotExists_ShouldReturn404() throws Exception {
        // Given: Service'den tenant bulunamadı hatası
        when(tenantService.updateTenant(eq(999L), any(TenantDto.class)))
                .thenThrow(new IllegalArgumentException("Kuaför bulunamadı"));

        // When & Then: PUT request
        mockMvc.perform(put("/tenants/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tenantDto)))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(tenantService).updateTenant(eq(999L), any(TenantDto.class));
    }

    @Test
    @DisplayName("DELETE /tenants/{id} - Kuaför deaktivasyon - Başarılı")
    void deactivateTenant_WhenExists_ShouldReturn204() throws Exception {
        // Given: Service başarıyla deaktive eder
        doNothing().when(tenantService).deactivateTenant(1L);

        // When & Then: DELETE request
        mockMvc.perform(delete("/tenants/{id}", 1L))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(tenantService).deactivateTenant(1L);
    }

    @Test
    @DisplayName("DELETE /tenants/{id} - Bulunamayan tenant için 404")
    void deactivateTenant_WhenNotExists_ShouldReturn404() throws Exception {
        // Given: Service'den tenant bulunamadı hatası
        doThrow(new IllegalArgumentException("Kuaför bulunamadı"))
                .when(tenantService).deactivateTenant(999L);

        // When & Then: DELETE request
        mockMvc.perform(delete("/tenants/{id}", 999L))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(tenantService).deactivateTenant(999L);
    }

    @Test
    @DisplayName("GET /tenants/search - İsme göre arama")
    void searchTenants_WithNameQuery_ShouldReturnFilteredList() throws Exception {
        // Given: Service'den filtrelenmiş liste döner
        List<TenantDto> searchResults = Arrays.asList(existingTenant);
        when(tenantService.searchByName("test")).thenReturn(searchResults);

        // When & Then: GET request with query parameter
        mockMvc.perform(get("/tenants/search")
                        .param("name", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Mevcut Kuaför")));

        verify(tenantService).searchByName("test");
    }

    @Test
    @DisplayName("GET /tenants/search - Boş query parametresi için boş liste")
    void searchTenants_WithEmptyQuery_ShouldReturnEmptyList() throws Exception {
        // Given: Service'den boş liste döner
        when(tenantService.searchByName("")).thenReturn(Arrays.asList());

        // When & Then: GET request with empty query
        mockMvc.perform(get("/tenants/search")
                        .param("name", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(tenantService).searchByName("");
    }

    @Test
    @DisplayName("GET /tenants/count - Aktif kuaför sayısı")
    void getTenantCount_ShouldReturnCount() throws Exception {
        // Given: Service'den sayı döner
        when(tenantService.countActiveTenants()).thenReturn(25L);

        // When & Then: GET request
        mockMvc.perform(get("/tenants/count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("25"));

        verify(tenantService).countActiveTenants();
    }

    @Test
    @DisplayName("GET /tenants/{id}/phone - WhatsApp numarası ile tenant ID bulma")
    void findTenantByPhoneNumber_WhenExists_ShouldReturnTenantId() throws Exception {
        // Given: Service'den tenant ID döner
        when(tenantService.findTenantIdByWhatsAppNumber("+905321234567")).thenReturn(1L);

        // When & Then: GET request
        mockMvc.perform(get("/tenants/internal/tenant-id-by-whatsapp/{phoneNumber}", "+905321234567")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("1"));

        verify(tenantService).findTenantIdByWhatsAppNumber("+905321234567");
    }

    @Test
    @DisplayName("GET /tenants/{id}/phone - Bulunamayan telefon için 404")
    void findTenantByPhoneNumber_WhenNotExists_ShouldReturn404() throws Exception {
        // Given: Service'den null döner
        when(tenantService.findTenantIdByWhatsAppNumber("+905329999999")).thenReturn(null);

        // When & Then: GET request
        mockMvc.perform(get("/tenants/internal/tenant-id-by-whatsapp/{phoneNumber}", "+905329999999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(tenantService).findTenantIdByWhatsAppNumber("+905329999999");
    }

    @Test
    @DisplayName("HTTP Method Not Allowed - 405")
    void unsupportedHttpMethod_ShouldReturn405() throws Exception {
        // When & Then: PATCH method (not supported)
        mockMvc.perform(patch("/tenants/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Content-Type kontrolü - Unsupported Media Type 415")
    void wrongContentType_ShouldReturn415() throws Exception {
        // When & Then: Wrong content type
        mockMvc.perform(post("/tenants")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text"))
                .andDo(print())
                .andExpect(status().isUnsupportedMediaType());
    }
}
