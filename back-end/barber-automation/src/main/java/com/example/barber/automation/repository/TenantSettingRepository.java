package com.example.barber.automation.repository;

import com.example.barber.automation.entity.TenantSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TenantSetting (Kuaför Ayarları) Repository
 */
@Repository
public interface TenantSettingRepository extends JpaRepository<TenantSetting, Long> {
    
    /**
     * Tenant'a ait belirli bir ayarı getirme
     */
    Optional<TenantSetting> findByTenantIdAndSettingKey(Long tenantId, String settingKey);
    
    /**
     * Tenant'a ait tüm ayarları getirme
     */
    List<TenantSetting> findByTenantIdOrderBySettingKeyAsc(Long tenantId);
    
    /**
     * Tenant'a ait belirli tipte ayarları getirme
     */
    List<TenantSetting> findByTenantIdAndType(Long tenantId, TenantSetting.SettingType type);
    
    /**
     * Tenant'a ait ayar anahtarına göre arama
     */
    @Query("SELECT ts FROM TenantSetting ts WHERE ts.tenant.id = :tenantId AND LOWER(ts.settingKey) LIKE LOWER(CONCAT('%', :key, '%')) ORDER BY ts.settingKey ASC")
    List<TenantSetting> findByTenantIdAndSettingKeyContainingIgnoreCase(@Param("tenantId") Long tenantId, @Param("key") String key);
    
    /**
     * Ayar anahtarının var olup olmadığını kontrol etme
     */
    boolean existsByTenantIdAndSettingKey(Long tenantId, String settingKey);
    
    /**
     * Çalışma saatleri ayarını getirme
     */
    @Query("SELECT ts.settingValue FROM TenantSetting ts WHERE ts.tenant.id = :tenantId AND ts.settingKey = 'working_hours'")
    Optional<String> findWorkingHoursByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Randevu aralığı ayarını getirme
     */
    @Query("SELECT ts.settingValue FROM TenantSetting ts WHERE ts.tenant.id = :tenantId AND ts.settingKey = 'booking_interval_minutes'")
    Optional<String> findBookingIntervalByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Hatırlatma gün sayısı ayarını getirme
     */
    @Query("SELECT ts.settingValue FROM TenantSetting ts WHERE ts.tenant.id = :tenantId AND ts.settingKey = 'reminder_days'")
    Optional<String> findReminderDaysByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * Mesaj şablonlarını getirme
     */
    @Query("SELECT ts FROM TenantSetting ts WHERE ts.tenant.id = :tenantId AND ts.settingKey LIKE '%_message' ORDER BY ts.settingKey ASC")
    List<TenantSetting> findMessageTemplatesByTenantId(@Param("tenantId") Long tenantId);
    
    /**
     * WhatsApp ayarlarını getirme
     */
    @Query("SELECT ts FROM TenantSetting ts WHERE ts.tenant.id = :tenantId AND ts.settingKey LIKE 'whatsapp_%' ORDER BY ts.settingKey ASC")
    List<TenantSetting> findWhatsAppSettingsByTenantId(@Param("tenantId") Long tenantId);
}
