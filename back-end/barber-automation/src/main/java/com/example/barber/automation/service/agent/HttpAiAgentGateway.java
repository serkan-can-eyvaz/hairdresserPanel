package com.example.barber.automation.service.agent;

import com.example.barber.automation.dto.AgentRespondRequest;
import com.example.barber.automation.dto.AgentRespondResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpAiAgentGateway implements AiAgentGateway {

    private static final Logger logger = LoggerFactory.getLogger(HttpAiAgentGateway.class);

    @Value("${ai.agent.base-url:http://127.0.0.1:4002}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public HttpAiAgentGateway(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public AgentRespondResponse respond(AgentRespondRequest request) {
        String url = baseUrl + "/v1/agent/respond";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AgentRespondRequest> httpEntity = new HttpEntity<>(request, headers);
        try {
            AgentRespondResponse resp = restTemplate.postForObject(url, httpEntity, AgentRespondResponse.class);
            return resp != null ? resp : new AgentRespondResponse(false, "error", "", null, null);
        } catch (Exception e) {
            logger.error("AI Agent HTTP çağrısı hatası", e);
            return new AgentRespondResponse(false, "error", "", null, null);
        }
    }
}


