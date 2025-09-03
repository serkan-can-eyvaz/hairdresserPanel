package com.example.barber.automation.controller;

import com.example.barber.automation.dto.TenantDto;
import com.example.barber.automation.entity.Tenant;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Simple TenantController REST API Test
 * 
 * Bu test sınıfı TenantController'ın temel HTTP endpoint'lerini test eder.
 * Security configuration olmadan sadece HTTP layer test edilir.
 */
@WebMvcTest(controllers = TenantController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
@DisplayName("Simple TenantController REST API Tests")
class SimpleTenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TenantService tenantService;

    // Test data
    private TenantDto tenantDto;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenantDto = new TenantDto();
        tenantDto.setId(1L);
        tenantDto.setName("Test Kuaför");
        tenantDto.setPhoneNumber("+905321234567");
        tenantDto.setActive(true);
        
        // Tenant entity oluştur
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Test Kuaför");
        tenant.setPhoneNumber("+905321234567");
        tenant.setActive(true);
    }

    @Test
    @DisplayName("GET /tenants - Kuaför listesi")
    void getAllTenants_ShouldReturnTenantList() throws Exception {
        // Given: Service'den tenant listesi döner
        List<TenantDto> tenants = Arrays.asList(tenantDto);
        when(tenantService.findAllActive()).thenReturn(tenants);

        // When & Then: GET request
        mockMvc.perform(get("/tenants")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Test Kuaför")));

        verify(tenantService).findAllActive();
    }

    @Test
    @DisplayName("GET /tenants/{id} - Tenant ID ile getirme - Başarılı")
    void getTenantById_WhenExists_ShouldReturnTenant() throws Exception {
        // Given: Service'den tenant döner
        when(tenantService.findById(1L)).thenReturn(Optional.of(tenantDto));

        // When & Then: GET request
        mockMvc.perform(get("/tenants/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Kuaför")));

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
    @DisplayName("GET /tenants/by-phone/{phoneNumber} - Telefon ile getirme")
    void getTenantByPhoneNumber_WhenExists_ShouldReturnTenant() throws Exception {
        // Given: Service'den tenant döner
        when(tenantService.findByPhoneNumber("+905321234567")).thenReturn(tenant);

        // When & Then: GET request
        mockMvc.perform(get("/tenants/by-phone/{phoneNumber}", "+905321234567")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.phoneNumber", is("+905321234567")));

        verify(tenantService).findByPhoneNumber("+905321234567");
    }

    @Test
    @DisplayName("POST /tenants - Yeni kuaför oluşturma - Başarılı")
    void createTenant_WithValidData_ShouldReturn201() throws Exception {
        // Given: Service'den yeni tenant döner
        TenantDto newTenantDto = new TenantDto();
        newTenantDto.setName("Yeni Kuaför");
        newTenantDto.setPhoneNumber("+905329876543");

        TenantDto createdTenant = new TenantDto();
        createdTenant.setId(2L);
        createdTenant.setName(newTenantDto.getName());
        createdTenant.setPhoneNumber(newTenantDto.getPhoneNumber());
        createdTenant.setActive(true);

        when(tenantService.createTenant(any(TenantDto.class))).thenReturn(createdTenant);

        // When & Then: POST request
        mockMvc.perform(post("/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTenantDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.name", is("Yeni Kuaför")))
                .andExpect(jsonPath("$.active", is(true)));

        verify(tenantService).createTenant(any(TenantDto.class));
    }
}
