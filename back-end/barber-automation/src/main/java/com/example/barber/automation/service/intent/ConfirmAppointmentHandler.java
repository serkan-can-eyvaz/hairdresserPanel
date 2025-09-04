package com.example.barber.automation.service.intent;

import com.example.barber.automation.dto.CreateAppointmentRequest;
import com.example.barber.automation.dto.AgentRespondResponse;
import com.example.barber.automation.dto.ServiceDto;
import com.example.barber.automation.service.AppointmentService;
import com.example.barber.automation.service.ServiceService;
import com.example.barber.automation.service.session.BotSessionService.BotSession;
import com.example.barber.automation.service.session.BotSessionService.BotState;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class ConfirmAppointmentHandler implements IntentHandler {

    private final AppointmentService appointmentService;
    private final ServiceService serviceService;

    public ConfirmAppointmentHandler(AppointmentService appointmentService, ServiceService serviceService) {
        this.appointmentService = appointmentService;
        this.serviceService = serviceService;
    }

    @Override
    public String intentKey() { return "confirm_appointment"; }

    @Override
    public void handle(BotSession session, AgentRespondResponse response) {
        if (session.getCustomerId() == null) return;
        Long tenantId = session.getSelectedTenantId() != null ? session.getSelectedTenantId() : session.getTenantId();
        if (tenantId == null) return;

        try {
            // Hizmet belirli değilse, kuaförün ilk aktif hizmetini kullan
            Long serviceId = session.getSelectedServiceIds().isEmpty() ? null : session.getSelectedServiceIds().get(0);
            if (serviceId == null) {
                List<ServiceDto> services = serviceService.findAllByTenant(tenantId);
                if (!services.isEmpty()) serviceId = services.get(0).getId();
            }
            if (serviceId == null) return;

            // Zaman belirli değilse, seçili tarih varsa 12:00, yoksa şimdi +1 saat
            LocalDateTime start = session.getSelectedTime();
            if (start == null) {
                LocalDate date = session.getSelectedDate();
                start = (date != null) ? date.atTime(12, 0) : LocalDateTime.now().plusHours(1);
            }

            CreateAppointmentRequest req = new CreateAppointmentRequest();
            req.setCustomerId(session.getCustomerId());
            req.setServiceId(serviceId);
            req.setStartTime(start);
            appointmentService.createAppointment(req, tenantId);
            session.setState(BotState.COMPLETED);
        } catch (Exception ignored) {
        }
    }
}


