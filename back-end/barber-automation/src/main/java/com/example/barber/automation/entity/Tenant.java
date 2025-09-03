package com.example.barber.automation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tenant (Kuaför) entity - Her kuaför için ayrı kayıt
 */
@Entity
@Table(name = "tenants")
public class Tenant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Kuaför adı boş olamaz")
    @Column(nullable = false, length = 100)
    private String name;
    
    @NotBlank(message = "Telefon numarası boş olamaz")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Geçerli bir telefon numarası giriniz (+905321234567)")
    @Column(nullable = false, unique = true, length = 20)
    private String phoneNumber;
    
    @Column(length = 200)
    private String address;
    
    @Column(length = 100)
    private String city; // Şehir
    
    @Column(length = 100)
    private String district; // İlçe

    @Column(length = 100)
    private String neighborhood; // Mahalle

    @Column(length = 300)
    private String addressDetail; // Adres detayı (cadde/kapı no vb.)
    
    @Column(length = 500)
    private String description;
    
    @Column(length = 50)
    private String timezone = "Europe/Istanbul";
    
    @Column(length = 100)
    private String email;
    
    @Column(length = 500)
    private String logoUrl;
    
    // Çalışma saatleri
    @Column(length = 10)
    private String workingHoursStart = "08:00"; // Başlangıç saati (HH:MM formatında)
    
    @Column(length = 10)
    private String workingHoursEnd = "22:00"; // Bitiş saati (HH:MM formatında)
    
    @Column(nullable = false)
    private Integer breakMinutes = 10; // Randevular arası mola süresi (dakika)
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // One-to-Many relationships
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TenantUser> users = new ArrayList<>();
    
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Service> services = new ArrayList<>();
    
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Customer> customers = new ArrayList<>();
    
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Appointment> appointments = new ArrayList<>();
    
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TenantSetting> settings = new ArrayList<>();
    
    // Constructors
    public Tenant() {}
    
    public Tenant(String name, String phoneNumber) {
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
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getPhone() {
        return phoneNumber;
    }
    
    public void setPhone(String phone) {
        this.phoneNumber = phone;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getLogoUrl() {
        return logoUrl;
    }
    
    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
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
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
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
    
    public List<TenantUser> getUsers() {
        return users;
    }
    
    public void setUsers(List<TenantUser> users) {
        this.users = users;
    }
    
    public List<Service> getServices() {
        return services;
    }
    
    public void setServices(List<Service> services) {
        this.services = services;
    }
    
    public List<Customer> getCustomers() {
        return customers;
    }
    
    public void setCustomers(List<Customer> customers) {
        this.customers = customers;
    }
    
    public List<Appointment> getAppointments() {
        return appointments;
    }
    
    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }
    
    public List<TenantSetting> getSettings() {
        return settings;
    }
    
    public void setSettings(List<TenantSetting> settings) {
        this.settings = settings;
    }
    
    @Override
    public String toString() {
        return "Tenant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", active=" + active +
                '}';
    }
}
