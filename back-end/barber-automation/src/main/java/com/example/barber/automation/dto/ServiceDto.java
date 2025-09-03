package com.example.barber.automation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kuaför hizmetleri için DTO
 */
public class ServiceDto {
    
    private Long id;
    private String name;
    private String description;
    private Integer durationMinutes;
    private BigDecimal price;
    private String currency;
    private Boolean active;
    private Integer sortOrder;
    private Long tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public ServiceDto() {}
    
    public ServiceDto(String name, Integer durationMinutes, BigDecimal price) {
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.currency = "TRY";
        this.active = true;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getDurationMinutes() {
        return durationMinutes;
    }
    
    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public Long getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
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
    
    public String getFormattedPrice() {
        if (price == null) return "Fiyat belirtilmemiş";
        return price + " " + currency;
    }
    
    @Override
    public String toString() {
        return "ServiceDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", price=" + price +
                ", active=" + active +
                '}';
    }
}
