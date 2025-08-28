package com.example.barber.automation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Hatırlatma kayıtları (30 gün sonra tekrar gel mesajları)
 */
@Entity
@Table(name = "reminders")
public class Reminder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Gönderilecek tarih boş olamaz")
    @Column(nullable = false)
    private LocalDateTime scheduledFor;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderStatus status = ReminderStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderType type = ReminderType.FOLLOW_UP;
    
    @Column(length = 1000)
    private String message;
    
    @Column
    private LocalDateTime sentAt;
    
    @Column(length = 500)
    private String errorMessage; // Hata durumunda mesaj
    
    @Column(nullable = false)
    private Integer retryCount = 0;
    
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
    @JoinColumn(name = "appointment_id")
    private Appointment appointment; // Opsiyonel - hangi randevuya bağlı
    
    public enum ReminderStatus {
        PENDING,   // Beklemede
        SENT,      // Gönderildi
        FAILED,    // Başarısız
        CANCELLED  // İptal edildi
    }
    
    public enum ReminderType {
        FOLLOW_UP,        // Takip hatırlatması (30 gün sonra)
        APPOINTMENT,      // Randevu hatırlatması (1 gün önce)
        BIRTHDAY,         // Doğum günü hatırlatması
        PROMOTION         // Promosyon/kampanya
    }
    
    // Constructors
    public Reminder() {}
    
    public Reminder(LocalDateTime scheduledFor, ReminderType type, 
                   Tenant tenant, Customer customer) {
        this.scheduledFor = scheduledFor;
        this.type = type;
        this.tenant = tenant;
        this.customer = customer;
    }
    
    public Reminder(LocalDateTime scheduledFor, ReminderType type, String message,
                   Tenant tenant, Customer customer) {
        this.scheduledFor = scheduledFor;
        this.type = type;
        this.message = message;
        this.tenant = tenant;
        this.customer = customer;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getScheduledFor() {
        return scheduledFor;
    }
    
    public void setScheduledFor(LocalDateTime scheduledFor) {
        this.scheduledFor = scheduledFor;
    }
    
    public ReminderStatus getStatus() {
        return status;
    }
    
    public void setStatus(ReminderStatus status) {
        this.status = status;
    }
    
    public ReminderType getType() {
        return type;
    }
    
    public void setType(ReminderType type) {
        this.type = type;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
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
    
    public Appointment getAppointment() {
        return appointment;
    }
    
    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }
    
    /**
     * Hatırlatmanın gönderilme zamanının gelip gelmediğini kontrol eder
     */
    public boolean isReady() {
        return status == ReminderStatus.PENDING && 
               scheduledFor.isBefore(LocalDateTime.now());
    }
    
    /**
     * Hatırlatmayı başarılı olarak işaretler
     */
    public void markAsSent() {
        this.status = ReminderStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }
    
    /**
     * Hatırlatmayı başarısız olarak işaretler
     */
    public void markAsFailed(String errorMessage) {
        this.status = ReminderStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }
    
    /**
     * Tekrar deneme sayısının sınırı aşıp aşmadığını kontrol eder
     */
    public boolean hasExceededRetryLimit() {
        return retryCount >= 3; // Maksimum 3 deneme
    }
    
    @Override
    public String toString() {
        return "Reminder{" +
                "id=" + id +
                ", scheduledFor=" + scheduledFor +
                ", status=" + status +
                ", type=" + type +
                ", customer=" + (customer != null ? customer.getName() : "null") +
                '}';
    }
}
