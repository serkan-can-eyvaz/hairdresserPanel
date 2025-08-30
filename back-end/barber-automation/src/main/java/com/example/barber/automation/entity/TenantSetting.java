package com.example.barber.automation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Kuaför ayarları (çalışma saatleri, mesaj şablonları, hatırlatma günleri vb.)
 */
@Entity
@Table(name = "tenant_settings")
public class TenantSetting {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Ayar anahtarı boş olamaz")
    @Column(nullable = false, length = 100)
    private String settingKey;
    
    @Column(columnDefinition = "TEXT")
    private String settingValue;
    
    @Column(length = 200)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettingType type = SettingType.STRING;
    
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
    
    public enum SettingType {
        STRING,           // Metin
        INTEGER,          // Sayı
        BOOLEAN,          // True/False
        JSON,             // JSON formatında karmaşık ayarlar
        BUSINESS_CONFIG,  // İş ayarları
        NOTIFICATION,     // Bildirim ayarları
        MESSAGE_TEMPLATE  // Mesaj şablonları
    }
    
    // Önceden tanımlı ayar anahtarları
    public static class Keys {
        // Çalışma saatleri
        public static final String WORKING_HOURS = "working_hours"; // JSON format
        public static final String BREAK_HOURS = "break_hours"; // JSON format
        
        // Randevu ayarları
        public static final String BOOKING_INTERVAL_MINUTES = "booking_interval_minutes"; // INTEGER
        public static final String MAX_ADVANCE_BOOKING_DAYS = "max_advance_booking_days"; // INTEGER
        public static final String MIN_ADVANCE_BOOKING_HOURS = "min_advance_booking_hours"; // INTEGER
        
        // Hatırlatma ayarları
        public static final String REMINDER_DAYS = "reminder_days"; // INTEGER
        public static final String REMINDER_ENABLED = "reminder_enabled"; // BOOLEAN
        
        // Mesaj şablonları
        public static final String WELCOME_MESSAGE = "welcome_message"; // STRING
        public static final String BOOKING_CONFIRMATION_MESSAGE = "booking_confirmation_message"; // STRING
        public static final String REMINDER_MESSAGE = "reminder_message"; // STRING
        public static final String CANCELLATION_MESSAGE = "cancellation_message"; // STRING
        
        // WhatsApp ayarları
        public static final String WHATSAPP_BUSINESS_NUMBER = "whatsapp_business_number"; // STRING
        public static final String WHATSAPP_WEBHOOK_TOKEN = "whatsapp_webhook_token"; // STRING
        
        // OpenAI ayarları
        public static final String OPENAI_ENABLED = "openai_enabled"; // BOOLEAN
        public static final String OPENAI_PROMPT_TEMPLATE = "openai_prompt_template"; // STRING
    }
    
    // Constructors
    public TenantSetting() {}
    
    public TenantSetting(String settingKey, String settingValue, Tenant tenant) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.tenant = tenant;
    }
    
    public TenantSetting(String settingKey, String settingValue, SettingType type, Tenant tenant) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.type = type;
        this.tenant = tenant;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSettingKey() {
        return settingKey;
    }
    
    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }
    
    public String getSettingValue() {
        return settingValue;
    }
    
    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public SettingType getType() {
        return type;
    }
    
    public void setType(SettingType type) {
        this.type = type;
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
    
    // Utility methods for type conversion
    public Integer getAsInteger() {
        if (settingValue == null) return null;
        try {
            return Integer.parseInt(settingValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    public Boolean getAsBoolean() {
        if (settingValue == null) return null;
        return Boolean.parseBoolean(settingValue);
    }
    
    public void setValue(Integer value) {
        this.settingValue = value != null ? value.toString() : null;
        this.type = SettingType.INTEGER;
    }
    
    public void setValue(Boolean value) {
        this.settingValue = value != null ? value.toString() : null;
        this.type = SettingType.BOOLEAN;
    }
    
    @Override
    public String toString() {
        return "TenantSetting{" +
                "id=" + id +
                ", settingKey='" + settingKey + '\'' +
                ", settingValue='" + settingValue + '\'' +
                ", type=" + type +
                '}';
    }
}
