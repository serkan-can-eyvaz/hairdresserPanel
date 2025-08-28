package com.example.barber.automation.dto;

import com.example.barber.automation.entity.Appointment;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Appointment (Randevu) DTO
 */
public class AppointmentDto {
    
    private Long id;
    
    @NotNull(message = "Başlangıç zamanı boş olamaz")
    private LocalDateTime startTime;
    
    @NotNull(message = "Bitiş zamanı boş olamaz")
    private LocalDateTime endTime;
    
    private Appointment.AppointmentStatus status;
    private String notes;
    private BigDecimal totalPrice;
    private String currency;
    private Boolean reminderSent;
    private LocalDateTime reminderSentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Related entities as IDs
    private Long tenantId;
    private Long customerId;
    private Long serviceId;
    
    // Related entities as DTOs (for detailed view)
    private CustomerDto customer;
    private ServiceDto service;
    
    // Constructors
    public AppointmentDto() {}
    
    public AppointmentDto(Long id, LocalDateTime startTime, LocalDateTime endTime, 
                         Appointment.AppointmentStatus status) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public Appointment.AppointmentStatus getStatus() {
        return status;
    }
    
    public void setStatus(Appointment.AppointmentStatus status) {
        this.status = status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
    
    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public Boolean getReminderSent() {
        return reminderSent;
    }
    
    public void setReminderSent(Boolean reminderSent) {
        this.reminderSent = reminderSent;
    }
    
    public LocalDateTime getReminderSentAt() {
        return reminderSentAt;
    }
    
    public void setReminderSentAt(LocalDateTime reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
    
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
    
    public CustomerDto getCustomer() {
        return customer;
    }
    
    public void setCustomer(CustomerDto customer) {
        this.customer = customer;
    }
    
    public ServiceDto getService() {
        return service;
    }
    
    public void setService(ServiceDto service) {
        this.service = service;
    }
    
    public String getFormattedPrice() {
        if (totalPrice == null) return "Fiyat belirtilmemiş";
        return totalPrice + " " + currency;
    }
}
