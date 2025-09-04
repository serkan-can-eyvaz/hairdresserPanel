package com.example.barber.automation.service.intent;

import com.example.barber.automation.dto.AgentRespondResponse;
import com.example.barber.automation.service.session.BotSessionService.BotSession;

public interface IntentHandler {
    String intentKey();
    void handle(BotSession session, AgentRespondResponse response);
}


