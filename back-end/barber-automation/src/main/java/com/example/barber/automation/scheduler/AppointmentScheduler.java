package com.example.barber.automation.scheduler;

import com.example.barber.automation.entity.Appointment;
import com.example.barber.automation.repository.AppointmentRepository;
import com.example.barber.automation.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Randevu sistemi için zamanlanmış görevler
 */
@Component
public class AppointmentScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(AppointmentScheduler.class);
    
    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;
    
    @Autowired
    public AppointmentScheduler(AppointmentRepository appointmentRepository,
                               NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.notificationService = notificationService;
    }
    
    /**
     * Tamamlanan randevular için hatırlatma oluşturma - Her gün saat 01:00'da
     */
    @Scheduled(cron = "0 0 1 * * ?") // Her gün saat 01:00
    @Transactional
    public void createRemindersForCompletedAppointments() {
        logger.info("Tamamlanan randevular için hatırlatma oluşturma görevi başlatıldı");
        
        try {
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            LocalDateTime startOfYesterday = yesterday.toLocalDate().atStartOfDay();
            LocalDateTime endOfYesterday = yesterday.toLocalDate().atTime(23, 59, 59);
            
            // Dün tamamlanan ve henüz hatırlatma gönderilmemiş randevuları bul
            List<Appointment> completedAppointments = appointmentRepository
                    .findByStartTimeBetweenAndStatusAndReminderSentFalse(
                            startOfYesterday, 
                            endOfYesterday, 
                            Appointment.AppointmentStatus.COMPLETED
                    );
            
            logger.info("Hatırlatma oluşturulacak randevu sayısı: {}", completedAppointments.size());
            
            for (Appointment appointment : completedAppointments) {
                try {
                    // Follow-up hatırlatması oluştur
                    notificationService.createFollowUpReminder(appointment);
                    
                    // Randevuyu hatırlatma gönderildi olarak işaretle
                    appointment.setReminderSent(true);
                    appointment.setReminderSentAt(LocalDateTime.now());
                    appointmentRepository.save(appointment);
                    
                    logger.debug("Hatırlatma oluşturuldu - Appointment ID: {}, Customer: {}", 
                            appointment.getId(), appointment.getCustomer().getName());
                    
                } catch (Exception e) {
                    logger.error("Randevu için hatırlatma oluşturulamadı - Appointment ID: {}", 
                            appointment.getId(), e);
                }
            }
            
            logger.info("Tamamlanan randevular için hatırlatma oluşturma görevi tamamlandı");
            
        } catch (Exception e) {
            logger.error("Hatırlatma oluşturma görevinde hata oluştu", e);
        }
    }
    
    /**
     * Geçmiş bekleyen randevuları "No Show" olarak işaretleme - Her 2 saatte bir
     */
    @Scheduled(fixedRate = 2 * 60 * 60 * 1000) // 2 saat
    @Transactional
    public void markMissedAppointments() {
        logger.debug("Kaçırılan randevu kontrolü başlatıldı");
        
        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            
            // 1 saatten fazla geçmiş ama hala beklemede/onaylanmış randevuları bul
            List<Appointment> missedAppointments = appointmentRepository
                    .findMissedAppointments(oneHourAgo);
            
            if (!missedAppointments.isEmpty()) {
                logger.info("Kaçırılan randevu sayısı: {}", missedAppointments.size());
                
                for (Appointment appointment : missedAppointments) {
                    appointment.setStatus(Appointment.AppointmentStatus.NO_SHOW);
                    appointmentRepository.save(appointment);
                    
                    logger.debug("Randevu 'No Show' olarak işaretlendi - Appointment ID: {}, Customer: {}", 
                            appointment.getId(), appointment.getCustomer().getName());
                }
            }
            
        } catch (Exception e) {
            logger.error("Kaçırılan randevu kontrolünde hata oluştu", e);
        }
    }
    
    /**
     * Yarınki randevular için hatırlatma mesajı gönderme - Her gün saat 18:00'da
     */
    @Scheduled(cron = "0 0 18 * * ?") // Her gün saat 18:00
    @Transactional
    public void sendTomorrowAppointmentReminders() {
        logger.info("Yarınki randevu hatırlatmaları gönderme görevi başlatıldı");
        
        try {
            LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
            LocalDateTime startOfTomorrow = tomorrow.toLocalDate().atStartOfDay();
            LocalDateTime endOfTomorrow = tomorrow.toLocalDate().atTime(23, 59, 59);
            
            // Yarınki onaylanmış randevuları bul
            List<Appointment> tomorrowAppointments = appointmentRepository
                    .findByStartTimeBetweenAndStatus(
                            startOfTomorrow, 
                            endOfTomorrow, 
                            Appointment.AppointmentStatus.CONFIRMED
                    );
            
            logger.info("Yarın için hatırlatma gönderilecek randevu sayısı: {}", tomorrowAppointments.size());
            
            for (Appointment appointment : tomorrowAppointments) {
                try {
                    // Randevu hatırlatma mesajı gönder
                    String reminderMessage = String.format(
                            "🔔 *Randevu Hatırlatması*\n\n" +
                            "Merhaba %s,\n\n" +
                            "Yarın saat %s için %s randevunuz bulunmaktadır.\n\n" +
                            "📍 Zamanında bekleriz! 😊",
                            appointment.getCustomer().getName(),
                            appointment.getStartTime().toLocalTime(),
                            appointment.getService().getName()
                    );
                    
                    // WhatsApp üzerinden gönder
                    // notificationService.sendAppointmentReminder(appointment, reminderMessage);
                    
                    logger.debug("Randevu hatırlatması gönderildi - Appointment ID: {}, Customer: {}", 
                            appointment.getId(), appointment.getCustomer().getName());
                    
                } catch (Exception e) {
                    logger.error("Randevu hatırlatması gönderilemedi - Appointment ID: {}", 
                            appointment.getId(), e);
                }
            }
            
            logger.info("Yarınki randevu hatırlatmaları gönderme görevi tamamlandı");
            
        } catch (Exception e) {
            logger.error("Randevu hatırlatma görevinde hata oluştu", e);
        }
    }
    
    /**
     * Sistem performans metrikleri - Her gün saat 23:00'da
     */
    @Scheduled(cron = "0 0 23 * * ?") // Her gün saat 23:00
    public void generateDailyMetrics() {
        logger.info("Günlük metrik toplama görevi başlatıldı");
        
        try {
            LocalDateTime today = LocalDateTime.now();
            LocalDateTime startOfDay = today.toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = today.toLocalDate().atTime(23, 59, 59);
            
            // Günlük istatistikler
            long todayCompletedAppointments = appointmentRepository
                    .countByStartTimeBetweenAndStatus(startOfDay, endOfDay, Appointment.AppointmentStatus.COMPLETED);
            long todayCancelledAppointments = appointmentRepository
                    .countByStartTimeBetweenAndStatus(startOfDay, endOfDay, Appointment.AppointmentStatus.CANCELLED);
            long todayNoShowAppointments = appointmentRepository
                    .countByStartTimeBetweenAndStatus(startOfDay, endOfDay, Appointment.AppointmentStatus.NO_SHOW);
            
            logger.info("Günlük istatistikler - Tamamlanan: {}, İptal: {}, Gelmedi: {}", 
                    todayCompletedAppointments, todayCancelledAppointments, todayNoShowAppointments);
            
            // TODO: Bu metrikleri database'e kaydetme veya monitoring sistemine gönderme
            
        } catch (Exception e) {
            logger.error("Günlük metrik toplama görevinde hata oluştu", e);
        }
    }
}
