package com.example.barber.automation.service;

import com.example.barber.automation.entity.Appointment;
import com.example.barber.automation.entity.Customer;
import com.example.barber.automation.entity.Reminder;
import com.example.barber.automation.entity.TenantSetting;
import com.example.barber.automation.repository.ReminderRepository;
import com.example.barber.automation.repository.TenantSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Bildirim ve hatırlatma servisi
 */
@Service
@Transactional
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final ReminderRepository reminderRepository;
    private final TenantSettingRepository tenantSettingRepository;
    private final WhatsAppService whatsAppService;
    
    @Autowired
    public NotificationService(ReminderRepository reminderRepository,
                              TenantSettingRepository tenantSettingRepository,
                              WhatsAppService whatsAppService) {
        this.reminderRepository = reminderRepository;
        this.tenantSettingRepository = tenantSettingRepository;
        this.whatsAppService = whatsAppService;
    }
    
    /**
     * Randevu onay mesajı gönderme
     */
    public void sendAppointmentConfirmation(Appointment appointment) {
        try {
            String message = buildConfirmationMessage(appointment);
            whatsAppService.sendMessage(
                    appointment.getCustomer().getPhoneNumber(),
                    message,
                    appointment.getTenant().getId()
            );
            
            logger.info("Randevu onay mesajı gönderildi - Appointment ID: {}, Customer: {}", 
                    appointment.getId(), appointment.getCustomer().getName());
        } catch (Exception e) {
            logger.error("Randevu onay mesajı gönderilemedi - Appointment ID: {}", 
                    appointment.getId(), e);
        }
    }
    
    /**
     * Hatırlatma mesajı oluşturma (30 gün sonra)
     */
    public void createFollowUpReminder(Appointment appointment) {
        // Hatırlatma ayarlarını kontrol et
        if (!isReminderEnabled(appointment.getTenant().getId())) {
            return;
        }
        
        int reminderDays = getReminderDays(appointment.getTenant().getId());
        
        // Aynı müşteri için bekleyen hatırlatma var mı kontrol et
        boolean hasPendingReminder = reminderRepository.hasPendingReminderForCustomer(
                appointment.getTenant().getId(),
                appointment.getCustomer().getId(),
                Reminder.ReminderType.FOLLOW_UP
        );
        
        if (hasPendingReminder) {
            logger.debug("Müşteri için zaten bekleyen hatırlatma var - Customer ID: {}", 
                    appointment.getCustomer().getId());
            return;
        }
        
        // Hatırlatma oluştur
        LocalDateTime scheduledFor = LocalDateTime.now().plusDays(reminderDays);
        String message = buildFollowUpMessage(appointment);
        
        Reminder reminder = new Reminder(
                scheduledFor,
                Reminder.ReminderType.FOLLOW_UP,
                message,
                appointment.getTenant(),
                appointment.getCustomer()
        );
        reminder.setAppointment(appointment);
        
        reminderRepository.save(reminder);
        
        logger.info("Hatırlatma oluşturuldu - Customer: {}, Scheduled for: {}", 
                appointment.getCustomer().getName(), scheduledFor);
    }
    
    /**
     * Bekleyen hatırlatmaları işleme
     */
    public void processReadyReminders() {
        List<Reminder> readyReminders = reminderRepository.findReadyToSendReminders(LocalDateTime.now());
        
        logger.info("İşlenecek hatırlatma sayısı: {}", readyReminders.size());
        
        for (Reminder reminder : readyReminders) {
            try {
                // Müşteri bildirim iznini kontrol et
                if (!reminder.getCustomer().getAllowNotifications()) {
                    reminder.setStatus(Reminder.ReminderStatus.CANCELLED);
                    reminderRepository.save(reminder);
                    continue;
                }
                
                // Mesajı gönder
                whatsAppService.sendMessage(
                        reminder.getCustomer().getPhoneNumber(),
                        reminder.getMessage(),
                        reminder.getTenant().getId()
                );
                
                // Başarılı olarak işaretle
                reminder.markAsSent();
                reminderRepository.save(reminder);
                
                logger.info("Hatırlatma gönderildi - Customer: {}, Type: {}", 
                        reminder.getCustomer().getName(), reminder.getType());
                
            } catch (Exception e) {
                // Hatalı olarak işaretle
                reminder.markAsFailed(e.getMessage());
                reminderRepository.save(reminder);
                
                logger.error("Hatırlatma gönderilemedi - Reminder ID: {}", 
                        reminder.getId(), e);
            }
        }
    }
    
    /**
     * Başarısız hatırlatmaları tekrar deneme
     */
    public void retryFailedReminders() {
        List<Reminder> failedReminders = reminderRepository.findFailedRemindersForRetry(LocalDateTime.now());
        
        logger.info("Tekrar denenecek hatırlatma sayısı: {}", failedReminders.size());
        
        for (Reminder reminder : failedReminders) {
            if (reminder.hasExceededRetryLimit()) {
                logger.warn("Hatırlatma deneme sınırı aşıldı - Reminder ID: {}", reminder.getId());
                continue;
            }
            
            try {
                whatsAppService.sendMessage(
                        reminder.getCustomer().getPhoneNumber(),
                        reminder.getMessage(),
                        reminder.getTenant().getId()
                );
                
                reminder.markAsSent();
                reminderRepository.save(reminder);
                
                logger.info("Hatırlatma tekrar deneme başarılı - Reminder ID: {}", reminder.getId());
                
            } catch (Exception e) {
                reminder.markAsFailed(e.getMessage());
                reminderRepository.save(reminder);
                
                logger.error("Hatırlatma tekrar deneme başarısız - Reminder ID: {}", 
                        reminder.getId(), e);
            }
        }
    }
    
    /**
     * Eski hatırlatmaları temizleme
     */
    public void cleanupOldReminders() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        List<Reminder> oldReminders = reminderRepository.findOldRemindersForCleanup(cutoffDate);
        
        if (!oldReminders.isEmpty()) {
            reminderRepository.deleteAll(oldReminders);
            logger.info("Eski hatırlatmalar temizlendi - Silinen sayı: {}", oldReminders.size());
        }
    }
    
    // Private helper methods
    
    private String buildConfirmationMessage(Appointment appointment) {
        // Özel mesaj şablonu var mı kontrol et
        Optional<TenantSetting> customMessage = tenantSettingRepository
                .findByTenantIdAndSettingKey(
                        appointment.getTenant().getId(),
                        TenantSetting.Keys.BOOKING_CONFIRMATION_MESSAGE
                );
        
        if (customMessage.isPresent()) {
            return customMessage.get().getSettingValue()
                    .replace("{{customer_name}}", appointment.getCustomer().getName())
                    .replace("{{service_name}}", appointment.getService().getName())
                    .replace("{{date}}", appointment.getStartTime().toLocalDate().toString())
                    .replace("{{time}}", appointment.getStartTime().toLocalTime().toString())
                    .replace("{{price}}", appointment.getFormattedPrice());
        }
        
        // Varsayılan mesaj
        return String.format(
                "✅ *Randevunuz Onaylandı!*\n\n" +
                "👤 *Ad:* %s\n" +
                "🔸 *Hizmet:* %s\n" +
                "📅 *Tarih:* %s\n" +
                "⏰ *Saat:* %s\n" +
                "💰 *Fiyat:* %s\n\n" +
                "📍 Randevu zamanında bekleriz! 😊",
                appointment.getCustomer().getName(),
                appointment.getService().getName(),
                appointment.getStartTime().toLocalDate(),
                appointment.getStartTime().toLocalTime(),
                appointment.getFormattedPrice()
        );
    }
    
    private String buildFollowUpMessage(Appointment appointment) {
        // Özel mesaj şablonu var mı kontrol et
        Optional<TenantSetting> customMessage = tenantSettingRepository
                .findByTenantIdAndSettingKey(
                        appointment.getTenant().getId(),
                        TenantSetting.Keys.REMINDER_MESSAGE
                );
        
        if (customMessage.isPresent()) {
            return customMessage.get().getSettingValue()
                    .replace("{{customer_name}}", appointment.getCustomer().getName())
                    .replace("{{service_name}}", appointment.getService().getName())
                    .replace("{{tenant_name}}", appointment.getTenant().getName());
        }
        
        // Varsayılan mesaj
        return String.format(
                "Merhaba %s! 👋\n\n" +
                "Son %s hizmetinizden bu yana bir süre geçti. " +
                "Tekrar bakım zamanınız geldi! 💇‍♂️\n\n" +
                "Randevu almak için mesaj atabilirsiniz. 📅\n\n" +
                "İyi günler! 😊",
                appointment.getCustomer().getName(),
                appointment.getService().getName()
        );
    }
    
    private boolean isReminderEnabled(Long tenantId) {
        Optional<TenantSetting> setting = tenantSettingRepository
                .findByTenantIdAndSettingKey(tenantId, TenantSetting.Keys.REMINDER_ENABLED);
        
        if (setting.isPresent()) {
            return setting.get().getAsBoolean() != null && setting.get().getAsBoolean();
        }
        
        return true; // Varsayılan olarak aktif
    }
    
    private int getReminderDays(Long tenantId) {
        Optional<TenantSetting> setting = tenantSettingRepository
                .findByTenantIdAndSettingKey(tenantId, TenantSetting.Keys.REMINDER_DAYS);
        
        if (setting.isPresent()) {
            Integer days = setting.get().getAsInteger();
            if (days != null && days > 0) {
                return days;
            }
        }
        
        return 30; // Varsayılan 30 gün
    }
}
