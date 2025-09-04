package com.example.barber.automation.service.intent;

import com.example.barber.automation.dto.CustomerDto;
import com.example.barber.automation.dto.AgentRespondResponse;
import com.example.barber.automation.service.CustomerService;
import com.example.barber.automation.service.session.BotSessionService.BotSession;
import com.example.barber.automation.service.session.BotSessionService.BotState;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProvideNameHandler implements IntentHandler {

    private final CustomerService customerService;

    public ProvideNameHandler(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public String intentKey() { return "provide_name"; }

    @Override
    public void handle(BotSession session, AgentRespondResponse response) {
        Map<String, Object> info = response.getExtractedInfo();
        if (info == null) return;
        Object nameObj = info.get("customer_name");
        if (nameObj == null) return;
        String name = String.valueOf(nameObj).trim();

        String phone = session.getPhoneNumber();
        if (!phone.startsWith("+")) phone = "+" + phone;
        // Seçilen kuaför varsa onun tenantId'si kullanılmalı
        Long targetTenantId = session.getSelectedTenantId() != null ? session.getSelectedTenantId() : session.getTenantId();
        CustomerDto customer = customerService.createCustomerFromWhatsApp(name, phone, targetTenantId);
        session.setCustomerId(customer.getId());
        session.setState(BotState.AWAITING_SERVICE);
    }
}


