package com.example.barber.automation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduled task konfigürasyonu
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Spring Boot otomatik olarak @Scheduled annotation'ları ile işaretlenmiş metotları çalıştırır
}
