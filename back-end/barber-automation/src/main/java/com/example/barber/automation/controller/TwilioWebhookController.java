package com.example.barber.automation.controller;

import com.example.barber.automation.dto.WhatsAppWebhookRequest;
import com.example.barber.automation.service.WhatsAppBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * Twilio WhatsApp Sandbox webhook endpoint.
 * Twilio, application/x-www-form-urlencoded olarak From / Body parametreleri gönderir.
 */
@RestController
@RequestMapping("/webhook/twilio")
@Tag(name = "Twilio Webhook", description = "Twilio WhatsApp webhook endpoint'i")
public class TwilioWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(TwilioWebhookController.class);

    private final WhatsAppBotService whatsAppBotService;

    public TwilioWebhookController(WhatsAppBotService whatsAppBotService) {
        this.whatsAppBotService = whatsAppBotService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Twilio'dan gelen mesajları alır")
    public ResponseEntity<String> receiveFromTwilio(
            @RequestParam(name = "From") String from,
            @RequestParam(name = "Body", required = false) String body,
            @RequestParam(name = "To", required = false) String to
    ) {
        try {
            logger.info("Twilio webhook mesajı alındı - From: {}, To: {}, Body: {}", from, to, body);

            // Twilio numarası whatsapp:+905.../whatsapp:+1415... biçimindedir. Sadece rakamları al.
            String fromNumber = from != null ? from.replace("whatsapp:", "").replace("+", "") : "";
            String businessPhone = (to != null ? to : "").replace("whatsapp:", "").replace("+", "");

            // WhatsAppWebhookRequest objesini minimal alanlarla oluştur
            WhatsAppWebhookRequest webhookRequest = new WhatsAppWebhookRequest();
            webhookRequest.setObject("whatsapp_business_account");

            WhatsAppWebhookRequest.Message message = new WhatsAppWebhookRequest.Message();
            message.setFrom(fromNumber);
            message.setType("text");
            WhatsAppWebhookRequest.Text text = new WhatsAppWebhookRequest.Text();
            text.setBody(body == null ? "" : body);
            message.setText(text);

            WhatsAppWebhookRequest.Value value = new WhatsAppWebhookRequest.Value();
            WhatsAppWebhookRequest.Metadata metadata = new WhatsAppWebhookRequest.Metadata();
            if (businessPhone != null && !businessPhone.isBlank()) {
                metadata.setDisplayPhoneNumber(businessPhone);
            }
            value.setMetadata(metadata);
            value.setMessages(Arrays.asList(message));

            WhatsAppWebhookRequest.Change change = new WhatsAppWebhookRequest.Change();
            change.setField("messages");
            change.setValue(value);

            WhatsAppWebhookRequest.Entry entry = new WhatsAppWebhookRequest.Entry();
            entry.setId("twilio");
            entry.setChanges(Arrays.asList(change));

            webhookRequest.setEntry(Arrays.asList(entry));

            // Mevcut bot akışına ilet
            whatsAppBotService.processIncomingMessage(webhookRequest);

            // Twilio'a 204 No Content dönelim; hiçbir metin dönmesin
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Twilio webhook işlenirken hata oluştu", e);
            return ResponseEntity.status(500).body("PROCESSING_ERROR");
        }
    }
}


