package com.example.barber.automation.service.intent;

import com.example.barber.automation.dto.AgentRespondResponse;
import com.example.barber.automation.service.session.BotSessionService.BotSession;
import com.example.barber.automation.service.session.BotSessionService.BotState;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
public class ProvideDateHandler implements IntentHandler {
    @Override
    public String intentKey() { return "provide_date"; }

    @Override
    public void handle(BotSession session, AgentRespondResponse response) {
        Map<String, Object> info = response.getExtractedInfo();
        if (info == null) return;
        Object datePref = info.get("date_preference");
        if (datePref == null) return;
        // YYYY-MM-DD ya da DD.MM.YYYY kabul et
        String raw = datePref.toString().trim();
        LocalDate parsed = null;
        try {
            if (raw.contains(".")) {
                String[] p = raw.split("\\.");
                if (p.length >= 3) {
                    parsed = LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
                }
            } else {
                parsed = LocalDate.parse(raw);
            }
        } catch (Exception ignored) {}
        if (parsed == null) parsed = LocalDate.now();
        session.setSelectedDate(parsed);
        session.setState(BotState.AWAITING_TIME);
    }
}


