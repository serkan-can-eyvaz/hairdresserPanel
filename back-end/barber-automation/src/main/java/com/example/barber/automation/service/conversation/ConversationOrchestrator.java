package com.example.barber.automation.service.conversation;

import com.example.barber.automation.dto.AgentRespondRequest;
import com.example.barber.automation.dto.AgentRespondResponse;
import com.example.barber.automation.service.agent.AiAgentGateway;
import com.example.barber.automation.service.session.BotSessionService;
import com.example.barber.automation.service.session.BotSessionService.BotSession;
import com.example.barber.automation.service.session.BotSessionService.BotState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ConversationOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(ConversationOrchestrator.class);

    private final AiAgentGateway aiAgentGateway;
    private final BotSessionService sessionService;

    private final java.util.List<com.example.barber.automation.service.intent.IntentHandler> handlers;

    public ConversationOrchestrator(AiAgentGateway aiAgentGateway, BotSessionService sessionService,
                                    java.util.List<com.example.barber.automation.service.intent.IntentHandler> handlers) {
        this.aiAgentGateway = aiAgentGateway;
        this.sessionService = sessionService;
        this.handlers = handlers;
    }

    public AgentRespondResponse handleIncoming(String phone, Long tenantId, String message) {
        BotSession session = sessionService.getOrCreate(phone, tenantId);

        // Hızlı onay algılama: AI'ya gitmeden randevu onayı
        if (session.getState() == BotState.AWAITING_CONFIRMATION) {
            String lower = message.toLowerCase();
            if (lower.contains("evet") || lower.contains("onay") || lower.contains("tamam") || lower.equals("e")) {
                AgentRespondResponse quick = new AgentRespondResponse(true, "confirm_appointment", "Onayınız alındı, randevunuz oluşturuluyor.", "completed", java.util.Collections.emptyMap());
                // Handler çalıştır
                for (var h : handlers) {
                    if (h.intentKey().equalsIgnoreCase(quick.getIntent())) {
                        h.handle(session, quick);
                        break;
                    }
                }
                return quick;
            }
        }

        AgentRespondRequest req = new AgentRespondRequest();
        req.setFrom_number(phone);
        req.setTenant_id(tenantId);
        req.setMessage(message);

        AgentRespondResponse resp = aiAgentGateway.respond(req);
        if (resp == null) return new AgentRespondResponse(false, "error", "", null, null);

        // State update (normalize Turkish chars issues)
        if (resp.getNextState() != null) {
            String s = resp.getNextState().toUpperCase()
                    .replace("İ", "I").replace("Ğ", "G").replace("Ü", "U")
                    .replace("Ş", "S").replace("Ö", "O").replace("Ç", "C");
            try {
                switch (s) {
                    case "AWAITING_LOCATION" -> session.setState(BotState.AWAITING_LOCATION);
                    case "AWAITING_BARBER_SELECTION" -> session.setState(BotState.AWAITING_BARBER_SELECTION);
                    case "AWAITING_NAME" -> session.setState(BotState.AWAITING_NAME);
                    case "AWAITING_SERVICE" -> session.setState(BotState.AWAITING_SERVICE);
                    case "AWAITING_DATE" -> session.setState(BotState.AWAITING_DATE);
                    case "AWAITING_TIME" -> session.setState(BotState.AWAITING_TIME);
                    case "AWAITING_CONFIRMATION" -> session.setState(BotState.AWAITING_CONFIRMATION);
                    case "COMPLETED" -> session.setState(BotState.COMPLETED);
                    default -> {}
                }
            } catch (Exception e) {
                logger.warn("State update failed: {}", e.getMessage());
            }
        }

        Map<String, Object> info = resp.getExtractedInfo();
        if (info != null) {
            if (info.containsKey("location_preference")) {
                session.setSelectedLocation(String.valueOf(info.get("location_preference")));
            }
            // Intent bazlı handler çalıştır
            for (var h : handlers) {
                if (h.intentKey().equalsIgnoreCase(resp.getIntent())) {
                    h.handle(session, resp);
                    break;
                }
            }
        }

        return resp;
    }
}


