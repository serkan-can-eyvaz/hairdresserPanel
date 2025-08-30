package com.example.barber.automation.repository;

import com.example.barber.automation.TestDataBuilder;
import com.example.barber.automation.entity.Tenant;
import com.example.barber.automation.entity.TenantSetting;
import com.example.barber.automation.entity.TenantSetting.SettingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantSettingRepository Integration Tests
 * JPA query doğrulaması ve veritabanı işlemleri testleri
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TenantSettingRepository Integration Tests")
class TenantSettingRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TenantSettingRepository tenantSettingRepository;

    private Tenant tenant1, tenant2;

    @BeforeEach
    void setUp() {
        // Test data oluştur
        tenant1 = TestDataBuilder.createTestTenant("Kuaför A", "+905321112233");
        tenant2 = TestDataBuilder.createTestTenant("Kuaför B", "+905376667788");
        
        entityManager.persistAndFlush(tenant1);
        entityManager.persistAndFlush(tenant2);
        
        // Test ayarları oluştur
        createTestSetting(tenant1, "working_hours", "{\"monday\":\"09:00-18:00\"}", SettingType.JSON);
        createTestSetting(tenant1, "reminder_days", "30", SettingType.INTEGER);
        createTestSetting(tenant1, "welcome_message", "Hoş geldiniz!", SettingType.STRING);
        
        createTestSetting(tenant2, "working_hours", "{\"monday\":\"10:00-19:00\"}", SettingType.JSON);
        createTestSetting(tenant2, "reminder_days", "15", SettingType.INTEGER);
        
        entityManager.flush();
        entityManager.clear();
    }

    private TenantSetting createTestSetting(Tenant tenant, String key, String value, SettingType type) {
        TenantSetting setting = new TenantSetting();
        setting.setTenant(tenant);
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setType(type);
        setting.setDescription("Test ayarı: " + key);
        
        return entityManager.persistAndFlush(setting);
    }

    @Test
    @DisplayName("Tenant ID ve setting key'e göre ayar bulma")
    void findByTenantIdAndSettingKey_WhenExists_ShouldReturnSetting() {
        // When
        Optional<TenantSetting> result = tenantSettingRepository.findByTenantIdAndSettingKey(
            tenant1.getId(), "working_hours");
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSettingKey()).isEqualTo("working_hours");
        assertThat(result.get().getSettingValue()).contains("09:00-18:00");
        assertThat(result.get().getTenant().getId()).isEqualTo(tenant1.getId());
    }

    @Test
    @DisplayName("Var olmayan setting key için empty döndürme")
    void findByTenantIdAndSettingKey_WhenNotExists_ShouldReturnEmpty() {
        // When
        Optional<TenantSetting> result = tenantSettingRepository.findByTenantIdAndSettingKey(
            tenant1.getId(), "non_existing_key");
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Tenant'a ait tüm ayarları listeleme")
    void findByTenantIdOrderBySettingKeyAsc_ShouldReturnAllSettings() {
        // When
        List<TenantSetting> result = tenantSettingRepository.findByTenantIdOrderBySettingKeyAsc(tenant1.getId());
        
        // Then
        assertThat(result).hasSize(3);
        assertThat(result.stream().map(TenantSetting::getSettingKey))
            .containsExactly("reminder_days", "welcome_message", "working_hours"); // alphabetical order
        assertThat(result.stream())
            .allMatch(setting -> setting.getTenant().getId().equals(tenant1.getId()));
    }

    @Test
    @DisplayName("Setting type'a göre filtreleme")
    void findByTenantIdAndType_ShouldReturnSettingsByType() {
        // When
        List<TenantSetting> jsonSettings = tenantSettingRepository.findByTenantIdAndType(
            tenant1.getId(), SettingType.JSON);
        List<TenantSetting> integerSettings = tenantSettingRepository.findByTenantIdAndType(
            tenant1.getId(), SettingType.INTEGER);
        List<TenantSetting> stringSettings = tenantSettingRepository.findByTenantIdAndType(
            tenant1.getId(), SettingType.STRING);
        
        // Then
        assertThat(jsonSettings).hasSize(1);
        assertThat(jsonSettings.get(0).getSettingKey()).isEqualTo("working_hours");
        
        assertThat(integerSettings).hasSize(1);
        assertThat(integerSettings.get(0).getSettingKey()).isEqualTo("reminder_days");
        
        assertThat(stringSettings).hasSize(1);
        assertThat(stringSettings.get(0).getSettingKey()).isEqualTo("welcome_message");
    }

    @Test
    @DisplayName("Setting key'de arama yapma (case insensitive)")
    void findByTenantIdAndSettingKeyContainingIgnoreCase_ShouldReturnMatchingSettings() {
        // When
        List<TenantSetting> result = tenantSettingRepository.findByTenantIdAndSettingKeyContainingIgnoreCase(
            tenant1.getId(), "working");
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSettingKey()).isEqualTo("working_hours");
    }

    @Test
    @DisplayName("Setting key varlığını kontrol etme")
    void existsByTenantIdAndSettingKey_ShouldReturnCorrectValue() {
        // When & Then
        assertThat(tenantSettingRepository.existsByTenantIdAndSettingKey(tenant1.getId(), "working_hours"))
            .isTrue();
        assertThat(tenantSettingRepository.existsByTenantIdAndSettingKey(tenant1.getId(), "non_existing"))
            .isFalse();
    }

    @Test
    @DisplayName("Setting value'da arama yapma")
    void findByTenantIdAndSettingValueContainingIgnoreCase_ShouldReturnMatchingSettings() {
        // When
        List<TenantSetting> result = tenantSettingRepository.findByTenantIdAndSettingValueContainingIgnoreCase(
            tenant1.getId(), "09:00");
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSettingKey()).isEqualTo("working_hours");
    }

    @Test
    @DisplayName("Var olmayan value arama")
    void findByTenantIdAndSettingValueContainingIgnoreCase_WhenNotExists_ShouldReturnEmpty() {
        // When
        List<TenantSetting> result = tenantSettingRepository.findByTenantIdAndSettingValueContainingIgnoreCase(
            tenant1.getId(), "non_existing_value");
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Çalışma saatleri ayarını getirme")
    void findWorkingHoursByTenantId_ShouldReturnWorkingHours() {
        // When
        Optional<String> result = tenantSettingRepository.findWorkingHoursByTenantId(tenant1.getId());
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).contains("09:00-18:00");
    }

    @Test
    @DisplayName("Booking interval ayarını getirme")
    void findBookingIntervalByTenantId_WhenNotExists_ShouldReturnEmpty() {
        // When
        Optional<String> result = tenantSettingRepository.findBookingIntervalByTenantId(tenant1.getId());
        
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Reminder days ayarını getirme")
    void findReminderDaysByTenantId_ShouldReturnReminderDays() {
        // When
        Optional<String> result = tenantSettingRepository.findReminderDaysByTenantId(tenant1.getId());
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("30");
    }

    @Test
    @DisplayName("Data isolation - Tenant'lar arası veri izolasyonu")
    void testDataIsolation_ShouldNotReturnOtherTenantSettings() {
        // When
        List<TenantSetting> tenant1Settings = tenantSettingRepository.findByTenantIdOrderBySettingKeyAsc(tenant1.getId());
        List<TenantSetting> tenant2Settings = tenantSettingRepository.findByTenantIdOrderBySettingKeyAsc(tenant2.getId());
        
        Optional<String> tenant1ReminderDays = tenantSettingRepository.findReminderDaysByTenantId(tenant1.getId());
        Optional<String> tenant2ReminderDays = tenantSettingRepository.findReminderDaysByTenantId(tenant2.getId());
        
        // Then
        assertThat(tenant1Settings).hasSize(3);
        assertThat(tenant2Settings).hasSize(2);
        
        assertThat(tenant1ReminderDays).isPresent();
        assertThat(tenant1ReminderDays.get()).isEqualTo("30");
        
        assertThat(tenant2ReminderDays).isPresent();
        assertThat(tenant2ReminderDays.get()).isEqualTo("15");
        
        // Verify cross-tenant isolation
        assertThat(tenant1Settings.stream())
            .allMatch(setting -> setting.getTenant().getId().equals(tenant1.getId()));
        assertThat(tenant2Settings.stream())
            .allMatch(setting -> setting.getTenant().getId().equals(tenant2.getId()));
    }

    @Test
    @DisplayName("Setting type utility methods test")
    void settingTypeUtilityMethods_ShouldWorkCorrectly() {
        // Given
        TenantSetting integerSetting = tenantSettingRepository.findByTenantIdAndSettingKey(
            tenant1.getId(), "reminder_days").orElseThrow();
        
        TenantSetting stringSetting = tenantSettingRepository.findByTenantIdAndSettingKey(
            tenant1.getId(), "welcome_message").orElseThrow();
        
        // When & Then
        assertThat(integerSetting.getAsInteger()).isEqualTo(30);
        assertThat(stringSetting.getAsInteger()).isNull(); // Invalid conversion
        
        // Test boolean setting
        TenantSetting booleanSetting = createTestSetting(tenant1, "test_boolean", "true", SettingType.BOOLEAN);
        entityManager.flush();
        
        assertThat(booleanSetting.getAsBoolean()).isTrue();
        
        // Test setValue methods
        booleanSetting.setValue(false);
        assertThat(booleanSetting.getType()).isEqualTo(SettingType.BOOLEAN);
        assertThat(booleanSetting.getAsBoolean()).isFalse();
        
        TenantSetting newIntegerSetting = new TenantSetting();
        newIntegerSetting.setValue(42);
        assertThat(newIntegerSetting.getType()).isEqualTo(SettingType.INTEGER);
        assertThat(newIntegerSetting.getAsInteger()).isEqualTo(42);
    }
}