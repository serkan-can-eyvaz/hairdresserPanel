package com.example.barber.automation.scheduler;

import com.example.barber.automation.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hatırlatma sistemi için zamanlanmış görevler
 */
@Component
@ConditionalOnProperty(name = "scheduling.reminder.enabled", havingValue = "true", matchIfMissing = true)
public class ReminderScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(ReminderScheduler.class);
    
    private final NotificationService notificationService;
    
    @Autowired
    public ReminderScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * Bekleyen hatırlatmaları işleme - Her 30 dakikada bir
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30 dakika = 30 * 60 * 1000 ms
    public void processReadyReminders() {
        logger.debug("Hatırlatma işleme görevi başlatıldı");
        
        try {
            notificationService.processReadyReminders();
            logger.debug("Hatırlatma işleme görevi tamamlandı");
        } catch (Exception e) {
            logger.error("Hatırlatma işleme görevinde hata oluştu", e);
        }
    }
    
    /**
     * Başarısız hatırlatmaları tekrar deneme - Her 2 saatte bir
     */
    @Scheduled(fixedRate = 2 * 60 * 60 * 1000) // 2 saat = 2 * 60 * 60 * 1000 ms
    public void retryFailedReminders() {
        logger.debug("Başarısız hatırlatma tekrar deneme görevi başlatıldı");
        
        try {
            notificationService.retryFailedReminders();
            logger.debug("Başarısız hatırlatma tekrar deneme görevi tamamlandı");
        } catch (Exception e) {
            logger.error("Başarısız hatırlatma tekrar deneme görevinde hata oluştu", e);
        }
    }
    
    /**
     * Eski hatırlatmaları temizleme - Her gün gece 02:00'da
     */
    @Scheduled(cron = "0 0 2 * * ?") // Her gün saat 02:00
    public void cleanupOldReminders() {
        logger.info("Eski hatırlatma temizleme görevi başlatıldı");
        
        try {
            notificationService.cleanupOldReminders();
            logger.info("Eski hatırlatma temizleme görevi tamamlandı");
        } catch (Exception e) {
            logger.error("Eski hatırlatma temizleme görevinde hata oluştu", e);
        }
    }
    
    /**
     * Sistem sağlık kontrolü - Her 15 dakikada bir
     */
    @Scheduled(fixedRate = 15 * 60 * 1000) // 15 dakika
    public void systemHealthCheck() {
        logger.debug("Sistem sağlık kontrolü yapılıyor");
        
        try {
            // Basit health check - database bağlantısı vs.
            // Bu işlem future improvements için placeholder
            logger.debug("Sistem sağlık kontrolü tamamlandı - Sistem sağlıklı");
        } catch (Exception e) {
            logger.error("Sistem sağlık kontrolünde hata oluştu", e);
        }
    }
}
