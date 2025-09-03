package com.example.barber.automation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Basit kuaför (tenant) oluşturma request DTO - Admin bilgileri olmadan
 */
public class CreateTenantSimpleRequest {
    
    @NotBlank(message = "Kuaför adı boş olamaz")
    @Size(max = 100, message = "Kuaför adı 100 karakterden uzun olamaz")
    private String name;
    
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

    // Constructors
    public CreateTenantSimpleRequest() {}

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

    @Override
    public String toString() {
        return "CreateTenantSimpleRequest{" +
                "name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", address='" + address + '\'' +
                ", email='" + email + '\'' +
                ", timezone='" + timezone + '\'' +
                '}';
    }
}
