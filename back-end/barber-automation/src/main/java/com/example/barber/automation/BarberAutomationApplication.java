package com.example.barber.automation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Multi-Tenant Kuaför Randevu Sistemi
 * 
 * - WhatsApp Bot entegrasyonu ile randevu alma
 * - Multi-tenant yapı (her kuaför kendi datasını görür)
 * - Otomatik hatırlatma sistemi
 * - RESTful API desteği
 */
@SpringBootApplication
@EnableScheduling
public class BarberAutomationApplication {

	public static void main(String[] args) {
		SpringApplication.run(BarberAutomationApplication.class, args);
		System.out.println("""
			🎉 Kuaför Randevu Sistemi Başlatıldı!
			
			📱 WhatsApp Bot: /webhook/whatsapp
			🔗 Swagger UI: http://localhost:8080/api/swagger-ui.html
			📊 API Docs: http://localhost:8080/api/api-docs
			
			✨ Multi-tenant yapı aktif
			⏰ Scheduled job'lar çalışıyor
			📲 WhatsApp entegrasyonu hazır
			""");
	}

}
