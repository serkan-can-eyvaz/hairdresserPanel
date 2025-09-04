package com.example.barber.automation.service.intent;

import com.example.barber.automation.dto.AgentRespondResponse;
import com.example.barber.automation.service.session.BotSessionService.BotSession;
import com.example.barber.automation.service.session.BotSessionService.BotState;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProvideTimeHandler implements IntentHandler {
    @Override
    public String intentKey() { return "provide_time"; }

    @Override
    public void handle(BotSession session, AgentRespondResponse response) {
        Map<String, Object> info = response.getExtractedInfo();
        if (info == null) return;
        Object timePref = info.get("time_preference");
        if (timePref == null) return;
        String raw = timePref.toString().trim();
        // 15:00 veya 1500
        int hour = 9, minute = 0;
        try {
            if (raw.contains(":")) {
                String[] p = raw.split(":");
                hour = Integer.parseInt(p[0]);
                minute = Integer.parseInt(p[1]);
            } else if (raw.length() >= 3) {
                hour = Integer.parseInt(raw.substring(0, 2));
                minute = Integer.parseInt(raw.substring(2, 4));
            }
        } catch (Exception ignored) {}
        if (session.getSelectedDate() != null) {
            session.setSelectedTime(session.getSelectedDate().atTime(hour, minute));
        }
        session.setState(BotState.AWAITING_CONFIRMATION);
    }
}


