package com.example.barber.automation.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Randevu oluşturma request DTO
 */
public class CreateAppointmentRequest {
    
    @NotNull(message = "Müşteri ID boş olamaz")
    private Long customerId;
    
    @NotNull(message = "Hizmet ID boş olamaz")
    private Long serviceId;
    
    @NotNull(message = "Başlangıç zamanı boş olamaz")
    private LocalDateTime startTime;
    
    private String notes;
    
    // WhatsApp entegrasyonu için
    private String customerName;
    private String customerPhone;
    
    // Constructors
    public CreateAppointmentRequest() {}
    
    public CreateAppointmentRequest(Long customerId, Long serviceId, LocalDateTime startTime) {
        this.customerId = customerId;
        this.serviceId = serviceId;
        this.startTime = startTime;
    }
    
    // Getters and Setters
    public Long getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
    
    public Long getServiceId() {
        return serviceId;
    }
    
    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getCustomerPhone() {
        return customerPhone;
    }
    
    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }
}
