package com.example.barber.automation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Twilio üzerinden WhatsApp mesajı gönderen basit servis.
 */
@Service
public class TwilioSendService {

    private static final Logger logger = LoggerFactory.getLogger(TwilioSendService.class);

    private final WebClient webClient;

    @Value("${twilio.accountSid}")
    private String accountSid;

    @Value("${twilio.authToken}")
    private String authToken;

    @Value("${twilio.apiBaseUrl:https://api.twilio.com/2010-04-01}")
    private String apiBaseUrl;

    @Value("${twilio.whatsapp.from}")
    private String fromWhatsApp;

    public TwilioSendService() {
        this.webClient = WebClient.builder().build();
    }

    public void sendWhatsAppText(String toWhatsApp, String body) {
        try {
            String url = String.format("%s/Accounts/%s/Messages.json", apiBaseUrl, accountSid);

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", toWhatsApp.startsWith("whatsapp:") ? toWhatsApp : "whatsapp:" + toWhatsApp);
            form.add("From", fromWhatsApp);
            form.add("Body", body);

            logger.info("Twilio send => URL: {}, To: {}, From: {}", url, toWhatsApp, fromWhatsApp);

            String resp = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .headers(h -> h.setBasicAuth(accountSid, authToken))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            logger.info("Twilio response: {}", resp);
        } catch (Exception e) {
            logger.error("Twilio mesaj gönderimi başarısız", e);
        }
    }
}


