package com.example.barber.automation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Yeni kuaför (tenant) oluşturma request DTO
 */
public class CreateTenantRequest {
    
    @NotBlank(message = "Kuaför adı boş olamaz")
    @Size(max = 100, message = "Kuaför adı 100 karakterden uzun olamaz")
    private String tenantName;
    
    @NotBlank(message = "Telefon numarası boş olamaz")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Geçerli bir telefon numarası giriniz (+905321234567)")
    private String phoneNumber;
    
    @Size(max = 200, message = "Adres 200 karakterden uzun olamaz")
    private String address;
    
    @Email(message = "Geçerli bir email adresi giriniz")
    @Size(max = 100, message = "Email 100 karakterden uzun olamaz")
    private String email;
    
    @Size(max = 50, message = "Timezone 50 karakterden uzun olamaz")
    private String timezone = "Europe/Istanbul";
    
    // Admin kullanıcı bilgileri
    @NotBlank(message = "Admin kullanıcı adı boş olamaz")
    @Size(min = 3, max = 50, message = "Kullanıcı adı 3-50 karakter arası olmalı")
    private String adminUsername;
    
    @NotBlank(message = "Admin email boş olamaz")
    @Email(message = "Geçerli bir admin email adresi giriniz")
    private String adminEmail;
    
    @NotBlank(message = "Admin şifre boş olamaz")
    @Size(min = 6, message = "Şifre en az 6 karakter olmalı")
    private String adminPassword;
    
    @NotBlank(message = "Admin adı boş olamaz")
    @Size(max = 50, message = "Ad 50 karakterden uzun olamaz")
    private String adminFirstName;
    
    @NotBlank(message = "Admin soyadı boş olamaz")
    @Size(max = 50, message = "Soyad 50 karakterden uzun olamaz")
    private String adminLastName;

    // Constructors
    public CreateTenantRequest() {}

    // Getters and Setters
    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAdminFirstName() {
        return adminFirstName;
    }

    public void setAdminFirstName(String adminFirstName) {
        this.adminFirstName = adminFirstName;
    }

    public String getAdminLastName() {
        return adminLastName;
    }

    public void setAdminLastName(String adminLastName) {
        this.adminLastName = adminLastName;
    }

    @Override
    public String toString() {
        return "CreateTenantRequest{" +
                "tenantName='" + tenantName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", adminUsername='" + adminUsername + '\'' +
                ", adminEmail='" + adminEmail + '\'' +
                '}';
    }
}
