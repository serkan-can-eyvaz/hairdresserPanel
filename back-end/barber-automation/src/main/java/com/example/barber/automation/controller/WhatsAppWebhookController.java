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
    
    @Value("${whatsapp.webhook-verify-token}")
    private String webhookVerifyToken;
    
    @Autowired
    public WhatsAppWebhookController(WhatsAppService whatsAppService, 
                                   WhatsAppBotService whatsAppBotService) {
        this.whatsAppService = whatsAppService;
        this.whatsAppBotService = whatsAppBotService;
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
    public ResponseEntity<String> receiveMessage(@RequestBody WhatsAppWebhookRequest webhookRequest) {
        try {
            logger.info("WhatsApp webhook mesajı alındı: {}", webhookRequest.getObject());
            
            // Gelen mesajları işle
            if (webhookRequest.getEntry() != null) {
                for (WhatsAppWebhookRequest.Entry entry : webhookRequest.getEntry()) {
                    processEntry(entry);
                }
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
                // Bot servisine mesajı ilet
                whatsAppBotService.processIncomingMessage(fromNumber, messageText, businessPhoneNumber);
            }
            
        } catch (Exception e) {
            logger.error("Gelen mesaj işlenirken hata oluştu", e);
        }
    }
}
