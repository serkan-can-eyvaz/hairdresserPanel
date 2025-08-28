package com.example.barber.automation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Randevu bilgileri
 */
@Entity
@Table(name = "appointments")
public class Appointment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Başlangıç zamanı boş olamaz")
    @Column(nullable = false)
    private LocalDateTime startTime;
    
    @NotNull(message = "Bitiş zamanı boş olamaz")
    @Column(nullable = false)
    private LocalDateTime endTime;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;
    
    @Column(length = 500)
    private String notes; // Randevu notu
    
    @Column(precision = 10, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(length = 10)
    private String currency = "TRY";
    
    @Column(nullable = false)
    private Boolean reminderSent = false;
    
    @Column
    private LocalDateTime reminderSentAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Many-to-One relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;
    
    public enum AppointmentStatus {
        PENDING,     // Beklemede
        CONFIRMED,   // Onaylandı
        COMPLETED,   // Tamamlandı
        CANCELLED,   // İptal edildi
        NO_SHOW      // Gelmedi
    }
    
    // Constructors
    public Appointment() {}
    
    public Appointment(LocalDateTime startTime, LocalDateTime endTime, 
                      Tenant tenant, Customer customer, Service service) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.tenant = tenant;
        this.customer = customer;
        this.service = service;
        this.totalPrice = service.getPrice();
        this.currency = service.getCurrency();
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
    
    public AppointmentStatus getStatus() {
        return status;
    }
    
    public void setStatus(AppointmentStatus status) {
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
    
    public Tenant getTenant() {
        return tenant;
    }
    
    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }
    
    public Customer getCustomer() {
        return customer;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public Service getService() {
        return service;
    }
    
    public void setService(Service service) {
        this.service = service;
    }
    
    /**
     * Randevunun tamamlanma durumunu kontrol eder
     */
    public boolean isCompleted() {
        return status == AppointmentStatus.COMPLETED;
    }
    
    /**
     * Randevunun iptal olup olmadığını kontrol eder
     */
    public boolean isCancelled() {
        return status == AppointmentStatus.CANCELLED || status == AppointmentStatus.NO_SHOW;
    }
    
    /**
     * Randevunun aktif olup olmadığını kontrol eder (iptal/tamamlanmamış)
     */
    public boolean isActive() {
        return status == AppointmentStatus.PENDING || status == AppointmentStatus.CONFIRMED;
    }
    
    /**
     * Formatlanmış fiyat bilgisini döner
     */
    public String getFormattedPrice() {
        if (totalPrice == null) return "Fiyat belirtilmemiş";
        return totalPrice + " " + currency;
    }
    
    @Override
    public String toString() {
        return "Appointment{" +
                "id=" + id +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status=" + status +
                ", customer=" + (customer != null ? customer.getName() : "null") +
                ", service=" + (service != null ? service.getName() : "null") +
                '}';
    }
}
