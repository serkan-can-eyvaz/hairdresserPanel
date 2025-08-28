package com.example.barber.automation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

/**
 * Customer (Müşteri) DTO
 */
public class CustomerDto {
    
    private Long id;
    
    @NotBlank(message = "Müşteri adı boş olamaz")
    private String name;
    
    @NotBlank(message = "Telefon numarası boş olamaz")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Geçerli bir telefon numarası giriniz (+905321234567)")
    private String phoneNumber;
    
    private String email;
    private String notes;
    private Boolean active;
    private Boolean allowNotifications;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private Long tenantId;
    
    // İstatistikler
    private LocalDateTime lastAppointmentDate;
    private Long totalAppointmentCount;
    
    // Constructors
    public CustomerDto() {}
    
    public CustomerDto(Long id, String name, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public Boolean getAllowNotifications() {
        return allowNotifications;
    }
    
    public void setAllowNotifications(Boolean allowNotifications) {
        this.allowNotifications = allowNotifications;
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
    
    public LocalDateTime getLastAppointmentDate() {
        return lastAppointmentDate;
    }
    
    public void setLastAppointmentDate(LocalDateTime lastAppointmentDate) {
        this.lastAppointmentDate = lastAppointmentDate;
    }
    
    public Long getTotalAppointmentCount() {
        return totalAppointmentCount;
    }
    
    public void setTotalAppointmentCount(Long totalAppointmentCount) {
        this.totalAppointmentCount = totalAppointmentCount;
    }
}
