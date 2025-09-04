package com.example.barber.automation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;

import java.util.List;

public class CreateTenantRequest {
    
    @NotBlank(message = "Kuaför adı gereklidir")
    private String name;
    
    @NotBlank(message = "Telefon numarası gereklidir")
    private String phoneNumber;
    
    @Email(message = "Geçerli bir email adresi giriniz")
    private String email;
    
    private String address;
    private String timezone = "Europe/Istanbul";
    private String city;
    private String district;
    private String neighborhood;
    private String addressDetail;
    private String workingHoursStart;
    private String workingHoursEnd;
    private Integer breakMinutes;
    
    @NotEmpty(message = "En az bir hizmet seçmelisiniz")
    @Valid
    private List<ServicePrice> services;
    
    // Getters and Setters
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
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getDistrict() {
        return district;
    }
    
    public void setDistrict(String district) {
        this.district = district;
    }
    
    public String getNeighborhood() {
        return neighborhood;
    }
    
    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }
    
    public String getAddressDetail() {
        return addressDetail;
    }
    
    public void setAddressDetail(String addressDetail) {
        this.addressDetail = addressDetail;
    }
    
    public String getWorkingHoursStart() {
        return workingHoursStart;
    }
    
    public void setWorkingHoursStart(String workingHoursStart) {
        this.workingHoursStart = workingHoursStart;
    }
    
    public String getWorkingHoursEnd() {
        return workingHoursEnd;
    }
    
    public void setWorkingHoursEnd(String workingHoursEnd) {
        this.workingHoursEnd = workingHoursEnd;
    }
    
    public Integer getBreakMinutes() {
        return breakMinutes;
    }
    
    public void setBreakMinutes(Integer breakMinutes) {
        this.breakMinutes = breakMinutes;
    }
    
    public List<ServicePrice> getServices() {
        return services;
    }
    
    public void setServices(List<ServicePrice> services) {
        this.services = services;
    }
    
    // Inner class for service price
    public static class ServicePrice {
        @NotNull(message = "Hizmet ID'si gereklidir")
        private Long serviceId;
        
        @NotNull(message = "Fiyat gereklidir")
        @Min(value = 0, message = "Fiyat 0'dan büyük olmalıdır")
        private Double price;
        
        @NotBlank(message = "Para birimi gereklidir")
        private String currency = "TRY";
        
        // Getters and Setters
        public Long getServiceId() {
            return serviceId;
        }
        
        public void setServiceId(Long serviceId) {
            this.serviceId = serviceId;
        }
        
        public Double getPrice() {
            return price;
        }
        
        public void setPrice(Double price) {
            this.price = price;
        }
        
        public String getCurrency() {
            return currency;
        }
        
        public void setCurrency(String currency) {
            this.currency = currency;
        }
    }
}