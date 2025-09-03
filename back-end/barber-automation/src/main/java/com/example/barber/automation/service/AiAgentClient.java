package com.example.barber.automation.service;

import com.example.barber.automation.dto.AgentRespondRequest;
import com.example.barber.automation.dto.AgentRespondResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AiAgentClient {

    private static final Logger logger = LoggerFactory.getLogger(AiAgentClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${aiagent.base-url:http://127.0.0.1:4002}")
    private String baseUrl;

    public AgentRespondResponse respond(AgentRespondRequest request) {
        String url = baseUrl + "/v1/agent/respond";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AgentRespondRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<AgentRespondResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    AgentRespondResponse.class
            );

            return response.getBody();
        } catch (Exception ex) {
            logger.error("AiAgent respond çağrısı başarısız", ex);
            AgentRespondResponse fallback = new AgentRespondResponse();
            fallback.setOk(false);
            fallback.setReply("Üzgünüm, şu an yardımcı olamıyorum. Lütfen daha sonra tekrar deneyin.");
            fallback.setIntent("error");
            return fallback;
        }
    }
}


