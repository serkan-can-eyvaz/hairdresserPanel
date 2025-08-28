package com.example.barber.automation;

import com.example.barber.automation.entity.*;
import com.example.barber.automation.entity.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Test verisi oluşturmak için helper sınıfı
 * 
 * Bu sınıf test senaryolarında kullanılacak entity'leri oluşturur.
 * Builder pattern kullanarak esnek ve okunabilir test verisi oluşturma sağlar.
 */
public class TestDataBuilder {
    
    /**
     * Test kuaförü oluşturur
     * 
     * @param name Kuaför adı
     * @param phoneNumber Telefon numarası (+90 formatında)
     * @return Test için hazırlanmış Tenant entity'si
     */
    public static Tenant createTestTenant(String name, String phoneNumber) {
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setPhoneNumber(phoneNumber);
        tenant.setAddress("Test Adres, İstanbul");
        tenant.setTimezone("Europe/Istanbul");
        tenant.setEmail(name.toLowerCase().replace(" ", "") + "@test.com");
        tenant.setActive(true);
        return tenant;
    }
    
    /**
     * Varsayılan test kuaförü oluşturur
     */
    public static Tenant createDefaultTestTenant() {
        return createTestTenant("Test Kuaför", "+905321234567");
    }
    
    /**
     * Test kuaför kullanıcısı oluşturur
     * 
     * @param username Kullanıcı adı
     * @param email Email adresi
     * @param tenant Bağlı olduğu kuaför
     * @param role Kullanıcı rolü
     * @return Test için hazırlanmış TenantUser entity'si
     */
    public static TenantUser createTestTenantUser(String username, String email, Tenant tenant, TenantUser.UserRole role) {
        TenantUser user = new TenantUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("hashedPassword123");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        user.setTenant(tenant);
        user.setActive(true);
        return user;
    }
    
    /**
     * Test müşterisi oluşturur
     * 
     * @param name Müşteri adı
     * @param phoneNumber Telefon numarası
     * @param tenant Bağlı olduğu kuaför
     * @return Test için hazırlanmış Customer entity'si
     */
    public static Customer createTestCustomer(String name, String phoneNumber, Tenant tenant) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setPhoneNumber(phoneNumber);
        customer.setEmail(name.toLowerCase().replace(" ", "") + "@customer.com");
        customer.setNotes("Test müşteri notu");
        customer.setTenant(tenant);
        customer.setActive(true);
        customer.setAllowNotifications(true);
        return customer;
    }
    
    /**
     * Test hizmeti oluşturur
     * 
     * @param name Hizmet adı
     * @param durationMinutes Süre (dakika)
     * @param price Fiyat
     * @param tenant Bağlı olduğu kuaför
     * @return Test için hazırlanmış Service entity'si
     */
    public static Service createTestService(String name, Integer durationMinutes, BigDecimal price, Tenant tenant) {
        Service service = new Service();
        service.setName(name);
        service.setDescription(name + " hizmeti açıklaması");
        service.setDurationMinutes(durationMinutes);
        service.setPrice(price);
        service.setCurrency("TRY");
        service.setTenant(tenant);
        service.setActive(true);
        service.setSortOrder(1);
        return service;
    }
    
    /**
     * Varsayılan test hizmetleri oluşturur
     */
    public static Service createDefaultHairCutService(Tenant tenant) {
        return createTestService("Saç Kesimi", 45, new BigDecimal("150.00"), tenant);
    }
    
    public static Service createDefaultBeardService(Tenant tenant) {
        return createTestService("Sakal Tıraşı", 30, new BigDecimal("100.00"), tenant);
    }
    
    /**
     * Test randevusu oluşturur
     * 
     * @param startTime Başlangıç zamanı
     * @param customer Müşteri
     * @param service Hizmet
     * @param tenant Kuaför
     * @return Test için hazırlanmış Appointment entity'si
     */
    public static Appointment createTestAppointment(LocalDateTime startTime, Customer customer, Service service, Tenant tenant) {
        LocalDateTime endTime = startTime.plusMinutes(service.getDurationMinutes());
        
        Appointment appointment = new Appointment();
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus(Appointment.AppointmentStatus.PENDING);
        appointment.setTotalPrice(service.getPrice());
        appointment.setCurrency(service.getCurrency());
        appointment.setCustomer(customer);
        appointment.setService(service);
        appointment.setTenant(tenant);
        appointment.setReminderSent(false);
        return appointment;
    }
    
    /**
     * Test kuaför ayarı oluşturur
     * 
     * @param key Ayar anahtarı
     * @param value Ayar değeri
     * @param type Ayar tipi
     * @param tenant Kuaför
     * @return Test için hazırlanmış TenantSetting entity'si
     */
    public static TenantSetting createTestTenantSetting(String key, String value, TenantSetting.SettingType type, Tenant tenant) {
        TenantSetting setting = new TenantSetting();
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        setting.setType(type);
        setting.setDescription("Test ayarı: " + key);
        setting.setTenant(tenant);
        return setting;
    }
    
    /**
     * Varsayılan çalışma saatleri ayarı oluşturur
     */
    public static TenantSetting createDefaultWorkingHoursSetting(Tenant tenant) {
        String workingHoursJson = """
            {
                "MONDAY": {"start": "09:00", "end": "18:00", "enabled": "true"},
                "TUESDAY": {"start": "09:00", "end": "18:00", "enabled": "true"},
                "WEDNESDAY": {"start": "09:00", "end": "18:00", "enabled": "true"},
                "THURSDAY": {"start": "09:00", "end": "18:00", "enabled": "true"},
                "FRIDAY": {"start": "09:00", "end": "18:00", "enabled": "true"},
                "SATURDAY": {"start": "10:00", "end": "17:00", "enabled": "true"},
                "SUNDAY": {"enabled": "false"}
            }
            """;
        return createTestTenantSetting(TenantSetting.Keys.WORKING_HOURS, workingHoursJson, TenantSetting.SettingType.JSON, tenant);
    }
    
    /**
     * Test hatırlatması oluşturur
     * 
     * @param scheduledFor Hatırlatma zamanı
     * @param type Hatırlatma tipi
     * @param customer Müşteri
     * @param tenant Kuaför
     * @return Test için hazırlanmış Reminder entity'si
     */
    public static Reminder createTestReminder(LocalDateTime scheduledFor, Reminder.ReminderType type, Customer customer, Tenant tenant) {
        Reminder reminder = new Reminder();
        reminder.setScheduledFor(scheduledFor);
        reminder.setType(type);
        reminder.setStatus(Reminder.ReminderStatus.PENDING);
        reminder.setMessage("Test hatırlatma mesajı");
        reminder.setCustomer(customer);
        reminder.setTenant(tenant);
        reminder.setRetryCount(0);
        return reminder;
    }
    
    /**
     * Test için birden fazla kuaför (multi-tenant test için)
     */
    public static class MultiTenantTestData {
        public final Tenant tenant1;
        public final Tenant tenant2;
        public final Customer customer1Tenant1;
        public final Customer customer2Tenant1;
        public final Customer customer1Tenant2;
        public final Service service1Tenant1;
        public final Service service1Tenant2;
        
        public MultiTenantTestData() {
            // İki farklı kuaför
            tenant1 = createTestTenant("Kuaför A", "+905321111111");
            tenant2 = createTestTenant("Kuaför B", "+905322222222");
            
            // Her kuaförün müşterileri
            customer1Tenant1 = createTestCustomer("Ali Veli", "+905331111111", tenant1);
            customer2Tenant1 = createTestCustomer("Ayşe Fatma", "+905332222222", tenant1);
            customer1Tenant2 = createTestCustomer("Mehmet Can", "+905333333333", tenant2);
            
            // Her kuaförün hizmetleri
            service1Tenant1 = createDefaultHairCutService(tenant1);
            service1Tenant2 = createDefaultHairCutService(tenant2);
        }
    }
}
