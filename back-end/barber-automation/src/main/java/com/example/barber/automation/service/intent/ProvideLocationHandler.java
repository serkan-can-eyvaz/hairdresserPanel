package com.example.barber.automation.service.intent;

import com.example.barber.automation.dto.AgentRespondResponse;
import com.example.barber.automation.dto.TenantDto;
import com.example.barber.automation.service.TenantService;
import com.example.barber.automation.service.session.BotSessionService;
import com.example.barber.automation.service.session.BotSessionService.BotSession;
import com.example.barber.automation.service.session.BotSessionService.BotState;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ProvideLocationHandler implements IntentHandler {

    private final TenantService tenantService;
    private final BotSessionService sessionService;

    public ProvideLocationHandler(TenantService tenantService, BotSessionService sessionService) {
        this.tenantService = tenantService;
        this.sessionService = sessionService;
    }

    @Override
    public String intentKey() { return "provide_location"; }

    @Override
    public void handle(BotSession session, AgentRespondResponse response) {
        Map<String, Object> info = response.getExtractedInfo();
        if (info == null) return;
        Object loc = info.get("location_preference");
        if (loc == null) return;

        String value = String.valueOf(loc);
        session.setSelectedLocation(value);

        String city = value;
        String district = null;
        if (value.contains(",")) {
            String[] parts = value.split(",", 2);
            city = parts[0].trim();
            district = parts[1].trim();
        }

        List<TenantDto> list = (district != null && !district.isEmpty())
                ? tenantService.findByCityAndDistrictDto(city, district)
                : tenantService.findByCityDto(city);

        session.setAvailableBarbers(list);
        session.setState(BotState.AWAITING_BARBER_SELECTION);
    }
}


