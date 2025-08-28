package com.example.barber.automation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * WhatsApp Business API entegrasyon servisi
 */
@Service
public class WhatsAppService {
    
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppService.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${whatsapp.api.url}")
    private String whatsappApiUrl;
    
    @Value("${whatsapp.api.token}")
    private String accessToken;
    
    public WhatsAppService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }
    
    /**
     * WhatsApp mesajı gönderme
     */
    public void sendMessage(String toPhoneNumber, String message, Long tenantId) {
        try {
            // Telefon numarası formatı kontrolü
            String formattedNumber = formatPhoneNumber(toPhoneNumber);
            
            // Mesaj payload'ı oluştur
            Map<String, Object> payload = createMessagePayload(formattedNumber, message);
            
            // API isteği gönder
            String response = webClient
                    .post()
                    .uri(whatsappApiUrl + "/messages")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(error -> logger.error("WhatsApp API hatası: {}", error.getMessage()))
                    .onErrorReturn("{\"error\": \"API call failed\"}")
                    .block();
            
            logger.info("WhatsApp mesajı gönderildi - To: {}, Tenant: {}, Response: {}", 
                    formattedNumber, tenantId, response);
            
        } catch (Exception e) {
            logger.error("WhatsApp mesajı gönderilemedi - To: {}, Tenant: {}", 
                    toPhoneNumber, tenantId, e);
            throw new RuntimeException("WhatsApp mesajı gönderilemedi", e);
        }
    }
    
    /**
     * WhatsApp interaktif mesaj gönderme (butonlarla)
     */
    public void sendInteractiveMessage(String toPhoneNumber, String text, 
                                     Map<String, String> buttons, Long tenantId) {
        try {
            String formattedNumber = formatPhoneNumber(toPhoneNumber);
            
            Map<String, Object> payload = createInteractiveMessagePayload(formattedNumber, text, buttons);
            
            String response = webClient
                    .post()
                    .uri(whatsappApiUrl + "/messages")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(error -> logger.error("WhatsApp interaktif mesaj hatası: {}", error.getMessage()))
                    .onErrorReturn("{\"error\": \"API call failed\"}")
                    .block();
            
            logger.info("WhatsApp interaktif mesajı gönderildi - To: {}, Tenant: {}", 
                    formattedNumber, tenantId);
            
        } catch (Exception e) {
            logger.error("WhatsApp interaktif mesajı gönderilemedi - To: {}, Tenant: {}", 
                    toPhoneNumber, tenantId, e);
            throw new RuntimeException("WhatsApp interaktif mesajı gönderilemedi", e);
        }
    }
    
    /**
     * Webhook doğrulama
     */
    public boolean verifyWebhook(String mode, String token, String challenge, String verifyToken) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            logger.info("Webhook doğrulandı");
            return true;
        }
        logger.warn("Webhook doğrulama başarısız - Mode: {}, Token: {}", mode, token);
        return false;
    }
    
    /**
     * Gelen mesajdan telefon numarasını ve tenant ID'yi çıkarma
     */
    public Long extractTenantIdFromWebhook(String businessPhoneNumber) {
        // TenantService üzerinden telefon numarasına göre tenant bulma
        // Bu metod webhook controller'da kullanılacak
        return null; // Placeholder - TenantService inject edilecek
    }
    
    // Private helper methods
    
    private String formatPhoneNumber(String phoneNumber) {
        // Telefon numarası formatlaması (uluslararası format)
        if (phoneNumber.startsWith("+")) {
            return phoneNumber.substring(1); // + işaretini kaldır
        }
        if (phoneNumber.startsWith("0")) {
            return "90" + phoneNumber.substring(1); // Türkiye için 90 ekle
        }
        return phoneNumber;
    }
    
    private Map<String, Object> createMessagePayload(String toPhoneNumber, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", toPhoneNumber);
        payload.put("type", "text");
        
        Map<String, String> text = new HashMap<>();
        text.put("body", message);
        payload.put("text", text);
        
        return payload;
    }
    
    private Map<String, Object> createInteractiveMessagePayload(String toPhoneNumber, 
                                                               String text, 
                                                               Map<String, String> buttons) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", toPhoneNumber);
        payload.put("type", "interactive");
        
        Map<String, Object> interactive = new HashMap<>();
        interactive.put("type", "button");
        
        // Header
        Map<String, Object> header = new HashMap<>();
        header.put("type", "text");
        header.put("text", "Randevu Sistemi");
        interactive.put("header", header);
        
        // Body
        Map<String, String> body = new HashMap<>();
        body.put("text", text);
        interactive.put("body", body);
        
        // Action (Buttons)
        Map<String, Object> action = new HashMap<>();
        Map<String, Object>[] buttonArray = new Map[buttons.size()];
        
        int index = 0;
        for (Map.Entry<String, String> entry : buttons.entrySet()) {
            Map<String, Object> button = new HashMap<>();
            button.put("type", "reply");
            
            Map<String, String> reply = new HashMap<>();
            reply.put("id", entry.getKey());
            reply.put("title", entry.getValue());
            button.put("reply", reply);
            
            buttonArray[index++] = button;
        }
        
        action.put("buttons", buttonArray);
        interactive.put("action", action);
        
        payload.put("interactive", interactive);
        
        return payload;
    }
    
    /**
     * WhatsApp mesaj şablonu gönderme (onaylanmış şablonlar için)
     */
    public void sendTemplateMessage(String toPhoneNumber, String templateName, 
                                   Map<String, String> parameters, Long tenantId) {
        try {
            String formattedNumber = formatPhoneNumber(toPhoneNumber);
            
            Map<String, Object> payload = createTemplateMessagePayload(formattedNumber, templateName, parameters);
            
            String response = webClient
                    .post()
                    .uri(whatsappApiUrl + "/messages")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(error -> logger.error("WhatsApp şablon mesaj hatası: {}", error.getMessage()))
                    .onErrorReturn("{\"error\": \"API call failed\"}")
                    .block();
            
            logger.info("WhatsApp şablon mesajı gönderildi - To: {}, Template: {}, Tenant: {}", 
                    formattedNumber, templateName, tenantId);
            
        } catch (Exception e) {
            logger.error("WhatsApp şablon mesajı gönderilemedi - To: {}, Template: {}, Tenant: {}", 
                    toPhoneNumber, templateName, tenantId, e);
            throw new RuntimeException("WhatsApp şablon mesajı gönderilemedi", e);
        }
    }
    
    private Map<String, Object> createTemplateMessagePayload(String toPhoneNumber, 
                                                            String templateName, 
                                                            Map<String, String> parameters) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", toPhoneNumber);
        payload.put("type", "template");
        
        Map<String, Object> template = new HashMap<>();
        template.put("name", templateName);
        template.put("language", Map.of("code", "tr")); // Türkçe
        
        if (parameters != null && !parameters.isEmpty()) {
            Map<String, Object>[] components = new Map[1];
            Map<String, Object> component = new HashMap<>();
            component.put("type", "body");
            
            Map<String, Object>[] paramArray = new Map[parameters.size()];
            int index = 0;
            for (String value : parameters.values()) {
                Map<String, Object> param = new HashMap<>();
                param.put("type", "text");
                param.put("text", value);
                paramArray[index++] = param;
            }
            
            component.put("parameters", paramArray);
            components[0] = component;
            template.put("components", components);
        }
        
        payload.put("template", template);
        
        return payload;
    }
}
