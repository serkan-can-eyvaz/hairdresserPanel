package com.example.barber.automation.controller;

import com.example.barber.automation.dto.WhatsAppWebhookRequest;
import com.example.barber.automation.service.WhatsAppBotService;
import com.example.barber.automation.service.WhatsAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * WhatsApp Webhook Controller
 */
@RestController
@RequestMapping("/webhook/whatsapp")
@Tag(name = "WhatsApp Webhook", description = "WhatsApp Business API webhook endpoint'leri")
public class WhatsAppWebhookController {
    
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);
    
    private final WhatsAppService whatsAppService;
    private final WhatsAppBotService whatsAppBotService;
    
    @Value("${whatsapp.api.webhook-verify-token}")
    private String webhookVerifyToken;
    
    @Autowired
    public WhatsAppWebhookController(WhatsAppService whatsAppService, 
                                   WhatsAppBotService whatsAppBotService) {
        this.whatsAppService = whatsAppService;
        this.whatsAppBotService = whatsAppBotService;
    }
    
    /**
     * Mock test endpoint - WhatsApp mesajı simülasyonu
     */
    @PostMapping("/mock")
    @Operation(summary = "Mock WhatsApp mesajı", description = "Test için mock WhatsApp mesajı gönder")
    public ResponseEntity<String> mockWhatsAppMessage(
            @Parameter(description = "Telefon numarası") @RequestParam("phone") String phone,
            @Parameter(description = "Mesaj") @RequestParam("message") String message,
            @Parameter(description = "Tenant ID") @RequestParam(value = "tenantId", defaultValue = "1") Long tenantId) {
        
        logger.info("=== MOCK WHATSAPP TEST ===");
        logger.info("Phone: {}", phone);
        logger.info("Message: {}", message);
        logger.info("Tenant ID: {}", tenantId);
        logger.info("========================");
        
        try {
            // Mock webhook request oluştur (Twilio formatında)
            String mockRawBody = String.format(
                "{\"From\":\"%s\",\"Body\":\"%s\",\"To\":\"whatsapp:+14155238886\"}", 
                phone, message
            );
            
            // Ana webhook endpoint'ini kullan
            return receiveMessage(mockRawBody);
        } catch (Exception e) {
            logger.error("Mock mesaj işleme hatası: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Hata: " + e.getMessage());
        }
    }
    
    /**
     * Webhook doğrulama (GET request)
     */
    @GetMapping
    @Operation(summary = "Webhook doğrulama", description = "WhatsApp webhook doğrulama endpoint'i")
    public ResponseEntity<String> verifyWebhook(
            @Parameter(description = "Webhook modu") @RequestParam("hub.mode") String mode,
            @Parameter(description = "Doğrulama token'ı") @RequestParam("hub.verify_token") String token,
            @Parameter(description = "Challenge kodu") @RequestParam("hub.challenge") String challenge) {
        
        logger.info("Webhook doğrulama isteği - Mode: {}, Token: {}", mode, token);
        
        if (whatsAppService.verifyWebhook(mode, token, challenge, webhookVerifyToken)) {
            logger.info("Webhook doğrulandı başarıyla");
            return ResponseEntity.ok(challenge);
        }
        
        logger.warn("Webhook doğrulama başarısız");
        return ResponseEntity.status(403).body("Forbidden");
    }
    
    /**
     * WhatsApp mesajlarını alma (POST request)
     */
    @PostMapping
    @Operation(summary = "WhatsApp mesajları", description = "WhatsApp'tan gelen mesajları işler")
    public ResponseEntity<String> receiveMessage(@RequestBody String rawBody) {
        try {
            logger.info("WhatsApp webhook raw body alındı: {}", rawBody);
            
            // Raw body'yi parse et
            if (rawBody.contains("whatsapp_business_account")) {
                // WhatsApp Business API formatı
                WhatsAppWebhookRequest webhookRequest = parseWhatsAppWebhook(rawBody);
                if (webhookRequest.getEntry() != null) {
                    for (WhatsAppWebhookRequest.Entry entry : webhookRequest.getEntry()) {
                        processEntry(entry);
                    }
                }
            } else {
                // Twilio formatı - direkt bot servisine gönder
                logger.info("Twilio formatı tespit edildi, bot servisine yönlendiriliyor");
                whatsAppBotService.processIncomingMessage(rawBody);
            }
            
            return ResponseEntity.ok("EVENT_RECEIVED");
            
        } catch (Exception e) {
            logger.error("WhatsApp webhook işlenirken hata oluştu", e);
            return ResponseEntity.status(500).body("PROCESSING_ERROR");
        }
    }
    
    /**
     * Manuel test mesajı gönderme
     */
    @PostMapping("/test")
    @Operation(summary = "Test mesajı", description = "Manuel test için WhatsApp mesajı gönderir")
    public ResponseEntity<String> sendTestMessage(
            @Parameter(description = "Alıcı telefon numarası") @RequestParam String to,
            @Parameter(description = "Mesaj içeriği") @RequestParam String message,
            @Parameter(description = "Kuaför ID'si") @RequestParam Long tenantId) {
        try {
            whatsAppService.sendMessage(to, message, tenantId);
            return ResponseEntity.ok("Test mesajı gönderildi");
        } catch (Exception e) {
            logger.error("Test mesajı gönderilemedi", e);
            return ResponseEntity.status(500).body("Mesaj gönderilemedi: " + e.getMessage());
        }
    }
    
    // Private helper methods
    
    private WhatsAppWebhookRequest parseWhatsAppWebhook(String rawBody) {
        try {
            // JSON parse et
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(rawBody, WhatsAppWebhookRequest.class);
        } catch (Exception e) {
            logger.error("WhatsApp webhook parse edilemedi", e);
            return new WhatsAppWebhookRequest();
        }
    }
    
    private void processEntry(WhatsAppWebhookRequest.Entry entry) {
        if (entry.getChanges() == null) return;
        
        for (WhatsAppWebhookRequest.Change change : entry.getChanges()) {
            if ("messages".equals(change.getField())) {
                processMessages(change.getValue());
            }
        }
    }
    
    private void processMessages(WhatsAppWebhookRequest.Value value) {
        if (value.getMessages() == null) return;
        
        String businessPhoneNumber = value.getMetadata().getDisplayPhoneNumber();
        
        for (WhatsAppWebhookRequest.Message message : value.getMessages()) {
            processIncomingMessage(message, businessPhoneNumber);
        }
    }
    
    private void processIncomingMessage(WhatsAppWebhookRequest.Message message, String businessPhoneNumber) {
        try {
            String fromNumber = message.getFrom();
            String messageText = message.getText() != null ? message.getText().getBody() : "";
            String messageType = message.getType();
            
            logger.info("Gelen mesaj - From: {}, Type: {}, Text: {}", fromNumber, messageType, messageText);
            
            // Sadece text mesajlarını işle
            if ("text".equals(messageType) && messageText != null && !messageText.trim().isEmpty()) {
                // Bot servisine mesajı ilet - WhatsAppWebhookRequest objesi oluştur
                WhatsAppWebhookRequest webhookRequest = new WhatsAppWebhookRequest();
                webhookRequest.setObject("whatsapp_business_account");
                
                WhatsAppWebhookRequest.Entry entry = new WhatsAppWebhookRequest.Entry();
                entry.setId("1");
                
                WhatsAppWebhookRequest.Change change = new WhatsAppWebhookRequest.Change();
                change.setField("messages");
                change.setValue(new WhatsAppWebhookRequest.Value());
                change.getValue().setMessages(Arrays.asList(message));
                change.getValue().setMetadata(new WhatsAppWebhookRequest.Metadata());
                change.getValue().getMetadata().setDisplayPhoneNumber(businessPhoneNumber);
                
                entry.setChanges(Arrays.asList(change));
                webhookRequest.setEntry(Arrays.asList(entry));
                
                whatsAppBotService.processIncomingMessage(webhookRequest);
            }
            
        } catch (Exception e) {
            logger.error("Gelen mesaj işlenirken hata oluştu", e);
        }
    }
}
