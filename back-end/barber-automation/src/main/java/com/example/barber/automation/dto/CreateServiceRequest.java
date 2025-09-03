package com.example.barber.automation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Yeni hizmet oluşturma request DTO
 */
public class CreateServiceRequest {
    
    @NotBlank(message = "Hizmet adı boş olamaz")
    @Size(max = 100, message = "Hizmet adı 100 karakterden uzun olamaz")
    private String name;
    
    @Size(max = 500, message = "Açıklama 500 karakterden uzun olamaz")
    private String description;
    
    @NotNull(message = "Süre boş olamaz")
    @Min(value = 1, message = "Süre en az 1 dakika olmalı")
    private Integer durationMinutes;
    
    @NotNull(message = "Fiyat boş olamaz")
    @Min(value = 0, message = "Fiyat negatif olamaz")
    private BigDecimal price;
    
    private String currency = "TRY";
    
    private Integer sortOrder = 0;
    
    // Constructors
    public CreateServiceRequest() {}
    
    public CreateServiceRequest(String name, Integer durationMinutes, BigDecimal price) {
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.price = price;
    }
    
    // Getters and Setters
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
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    @Override
    public String toString() {
        return "CreateServiceRequest{" +
                "name='" + name + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", price=" + price +
                ", currency='" + currency + '\'' +
                '}';
    }
}
