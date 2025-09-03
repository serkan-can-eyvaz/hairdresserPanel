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
 * Müşteri bilgileri
 */
@Entity
@Table(name = "customers")
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Müşteri adı boş olamaz")
    @Column(nullable = false, length = 100)
    private String name;
    
    @NotBlank(message = "Telefon numarası boş olamaz")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Geçerli bir telefon numarası giriniz (905321234567 veya +905321234567)")
    @Column(nullable = false, length = 20)
    private String phoneNumber;
    
    @Column(length = 100)
    private String email;
    
    @Column(length = 500)
    private String notes; // Kuaför notu (alerjiler, tercihler vs.)
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(nullable = false)
    private Boolean allowNotifications = true; // WhatsApp bildirimleri
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Many-to-One relationship with Tenant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    
    // One-to-Many relationship with Appointments
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Appointment> appointments = new ArrayList<>();
    
    // Constructors
    public Customer() {}
    
    public Customer(String name, String phoneNumber, Tenant tenant) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.tenant = tenant;
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
    
    public Tenant getTenant() {
        return tenant;
    }
    
    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }
    
    public List<Appointment> getAppointments() {
        return appointments;
    }
    
    public void setAppointments(List<Appointment> appointments) {
        this.appointments = appointments;
    }
    
    /**
     * Son randevu tarihini döner
     */
    public LocalDateTime getLastAppointmentDate() {
        return appointments.stream()
                .filter(appointment -> appointment.getStatus() == Appointment.AppointmentStatus.COMPLETED)
                .map(Appointment::getStartTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }
    
    /**
     * Toplam randevu sayısını döner
     */
    public long getTotalAppointmentCount() {
        return appointments.stream()
                .filter(appointment -> appointment.getStatus() == Appointment.AppointmentStatus.COMPLETED)
                .count();
    }
    
    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", active=" + active +
                '}';
    }
}
