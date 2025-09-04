package com.example.barber.automation.service.intent;

import com.example.barber.automation.dto.AgentRespondResponse;
import com.example.barber.automation.dto.TenantDto;
import com.example.barber.automation.service.session.BotSessionService.BotSession;
import com.example.barber.automation.service.session.BotSessionService.BotState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SelectBarberHandler implements IntentHandler {
    @Override
    public String intentKey() { return "select_barber"; }

    @Override
    public void handle(BotSession session, AgentRespondResponse response) {
        Map<String, Object> info = response.getExtractedInfo();
        if (info == null) return;
        Object selectionObj = info.get("barber_selection");
        if (selectionObj == null) return;
        int index;
        try { index = Integer.parseInt(String.valueOf(selectionObj)) - 1; } catch (Exception e) { return; }

        // AI’den liste geldiyse onu kullan
        if (info.containsKey("barber_options")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> options = (List<Map<String, Object>>) info.get("barber_options");
            if (index >= 0 && index < options.size()) {
                Object idObj = options.get(index).get("id");
                if (idObj instanceof Number) {
                    session.setSelectedTenantId(((Number) idObj).longValue());
                    session.setState(BotState.AWAITING_NAME);
                    return;
                }
            }
        }

        // Aksi halde session’daki listeyi kullan
        List<TenantDto> available = session.getAvailableBarbers();
        if (available != null && index >= 0 && index < available.size()) {
            session.setSelectedTenantId(available.get(index).getId());
            session.setState(BotState.AWAITING_NAME);
        }
    }
}


