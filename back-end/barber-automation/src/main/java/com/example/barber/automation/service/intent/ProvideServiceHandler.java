package com.example.barber.automation.service.intent;

import com.example.barber.automation.dto.ServiceDto;
import com.example.barber.automation.dto.AgentRespondResponse;
import com.example.barber.automation.service.ServiceService;
import com.example.barber.automation.service.session.BotSessionService.BotSession;
import com.example.barber.automation.service.session.BotSessionService.BotState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Component
public class ProvideServiceHandler implements IntentHandler {

    private final ServiceService serviceService;

    public ProvideServiceHandler(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @Override
    public String intentKey() { return "provide_service"; }

    @Override
    public void handle(BotSession session, AgentRespondResponse response) {
        Map<String, Object> info = response.getExtractedInfo();
        if (info == null) return;
        Object servicePref = info.get("service_preference");
        if (servicePref == null) return;

        List<ServiceDto> tenantServices = serviceService.findAllByTenant(session.getSelectedTenantId() != null ? session.getSelectedTenantId() : session.getTenantId());
        // Basit eşleme: adı geçen ilk hizmet(ler)i topla
        String pref = servicePref.toString().toLowerCase();
        session.getSelectedServiceIds().clear();
        int totalDuration = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;
        String currency = null;
        for (ServiceDto s : tenantServices) {
            String name = s.getName() != null ? s.getName().toLowerCase() : "";
            if (!name.isEmpty() && pref.contains(name.split(" ")[0])) { // kaba eşleme
                session.getSelectedServiceIds().add(s.getId());
                totalDuration += s.getDurationMinutes() != null ? s.getDurationMinutes() : 0;
                if (s.getPrice() != null) totalPrice = totalPrice.add(s.getPrice());
                if (currency == null && s.getCurrency() != null) currency = s.getCurrency();
            }
        }
        if (session.getSelectedServiceIds().isEmpty() && !tenantServices.isEmpty()) {
            // Hiç eşleşme yoksa ilk hizmeti al
            ServiceDto first = tenantServices.get(0);
            session.getSelectedServiceIds().add(first.getId());
            totalDuration = first.getDurationMinutes() != null ? first.getDurationMinutes() : 0;
            totalPrice = first.getPrice() != null ? first.getPrice() : BigDecimal.ZERO;
            currency = first.getCurrency();
        }
        session.setTotalDurationMinutes(totalDuration);
        session.setTotalPrice(totalPrice);
        session.setTotalCurrency(currency);
        session.setState(BotState.AWAITING_DATE);
    }
}


