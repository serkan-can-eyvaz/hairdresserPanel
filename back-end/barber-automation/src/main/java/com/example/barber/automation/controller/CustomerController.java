package com.example.barber.automation.controller;

import com.example.barber.automation.dto.CustomerDto;
import com.example.barber.automation.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Customer (Müşteri) REST API Controller
 */
@RestController
@RequestMapping("/customers")
@Tag(name = "Customer Management", description = "Müşteri yönetimi API'leri")
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Tüm müşterileri listele
     */
    @GetMapping
    @Operation(summary = "Müşteri listesi", description = "Tüm aktif müşterileri listeler")
    public ResponseEntity<List<CustomerDto>> getAllCustomers(
            @Parameter(description = "Tenant ID") @RequestParam(value = "tenantId", required = false) Long tenantId) {
        
        List<CustomerDto> customers;
        if (tenantId != null) {
            customers = customerService.findAllByTenantId(tenantId);
        } else {
            customers = customerService.findAll();
        }
        
        return ResponseEntity.ok(customers);
    }

    /**
     * ID'ye göre müşteri getir
     */
    @GetMapping("/{id}")
    @Operation(summary = "Müşteri detayı", description = "ID'ye göre müşteri detayını getirir")
    public ResponseEntity<CustomerDto> getCustomerById(
            @Parameter(description = "Müşteri ID") @PathVariable Long id,
            @Parameter(description = "Tenant ID") @RequestParam(value = "tenantId", required = false) Long tenantId) {
        
        if (tenantId == null) {
            tenantId = 1L; // Varsayılan tenant ID
        }
        
        return customerService.findById(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Telefon numarasına göre müşteri bul
     */
    @GetMapping("/by-phone")
    @Operation(summary = "Telefon ile müşteri bul", description = "Telefon numarasına göre müşteri bulur")
    public ResponseEntity<CustomerDto> getCustomerByPhone(
            @Parameter(description = "Telefon numarası") @RequestParam String phoneNumber,
            @Parameter(description = "Tenant ID") @RequestParam(value = "tenantId", required = false) Long tenantId) {
        
        if (tenantId == null) {
            tenantId = 1L; // Varsayılan tenant ID
        }
        
        return customerService.findByPhoneNumber(phoneNumber, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Yeni müşteri oluştur
     */
    @PostMapping
    @Operation(summary = "Yeni müşteri", description = "Yeni müşteri oluşturur")
    public ResponseEntity<CustomerDto> createCustomer(
            @RequestBody CustomerDto customerDto,
            @Parameter(description = "Tenant ID") @RequestParam(value = "tenantId", required = false) Long tenantId) {
        
        if (tenantId == null) {
            tenantId = 1L; // Varsayılan tenant ID
        }
        
        try {
            CustomerDto createdCustomer = customerService.createCustomer(customerDto, tenantId);
            return ResponseEntity.ok(createdCustomer);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Müşteri güncelle
     */
    @PutMapping("/{id}")
    @Operation(summary = "Müşteri güncelle", description = "Mevcut müşteriyi günceller")
    public ResponseEntity<CustomerDto> updateCustomer(
            @Parameter(description = "Müşteri ID") @PathVariable Long id,
            @RequestBody CustomerDto customerDto,
            @Parameter(description = "Tenant ID") @RequestParam(value = "tenantId", required = false) Long tenantId) {
        
        if (tenantId == null) {
            tenantId = 1L; // Varsayılan tenant ID
        }
        
        try {
            CustomerDto updatedCustomer = customerService.updateCustomer(id, customerDto, tenantId);
            return ResponseEntity.ok(updatedCustomer);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Müşteri sil (soft delete)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Müşteri sil", description = "Müşteriyi siler (soft delete)")
    public ResponseEntity<Void> deleteCustomer(
            @Parameter(description = "Müşteri ID") @PathVariable Long id,
            @Parameter(description = "Tenant ID") @RequestParam(value = "tenantId", required = false) Long tenantId) {
        
        if (tenantId == null) {
            tenantId = 1L; // Varsayılan tenant ID
        }
        
        try {
            customerService.deleteCustomer(id, tenantId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
