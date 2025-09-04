package com.example.barber.automation.service.agent;

import com.example.barber.automation.dto.AgentRespondRequest;
import com.example.barber.automation.dto.AgentRespondResponse;

/**
 * GPT-4o ile konuşmayı soyutlayan arayüz.
 */
public interface AiAgentGateway {

    AgentRespondResponse respond(AgentRespondRequest request);
}


