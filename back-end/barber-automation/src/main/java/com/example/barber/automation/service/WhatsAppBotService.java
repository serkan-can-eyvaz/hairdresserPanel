package com.example.barber.automation.service;

import com.example.barber.automation.dto.CreateAppointmentRequest;
import com.example.barber.automation.dto.CustomerDto;
import com.example.barber.automation.dto.ServiceDto;
import com.example.barber.automation.dto.SlotResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * WhatsApp Bot işlem servisi
 */
@Service
public class WhatsAppBotService {
    
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppBotService.class);
    
    private final TenantService tenantService;
    private final CustomerService customerService;
    private final ServiceService serviceService;
    private final SlotService slotService;
    private final AppointmentService appointmentService;
    private final WhatsAppService whatsAppService;
    
    // Bot state management (basit in-memory cache)
    private final Map<String, BotSession> userSessions = new HashMap<>();
    
    @Autowired
    public WhatsAppBotService(TenantService tenantService,
                             CustomerService customerService,
                             ServiceService serviceService,
                             SlotService slotService,
                             AppointmentService appointmentService,
                             WhatsAppService whatsAppService) {
        this.tenantService = tenantService;
        this.customerService = customerService;
        this.serviceService = serviceService;
        this.slotService = slotService;
        this.appointmentService = appointmentService;
        this.whatsAppService = whatsAppService;
    }
    
    /**
     * Gelen WhatsApp mesajını işleme
     */
    public void processIncomingMessage(String fromNumber, String messageText, String businessPhoneNumber) {
        try {
            // Tenant ID'yi bul
            Long tenantId = tenantService.findTenantIdByWhatsAppNumber(businessPhoneNumber);
            if (tenantId == null) {
                logger.warn("Bilinmeyen business numarası: {}", businessPhoneNumber);
                return;
            }
            
            // Session yönetimi
            String sessionKey = fromNumber + "_" + tenantId;
            BotSession session = userSessions.computeIfAbsent(sessionKey, k -> new BotSession(fromNumber, tenantId));
            
            // Mesajı işle ve yanıtla
            processMessage(session, messageText.trim());
            
        } catch (Exception e) {
            logger.error("WhatsApp bot mesajı işlenirken hata oluştu", e);
            
            // Hata durumunda kullanıcıya genel mesaj gönder
            try {
                whatsAppService.sendMessage(fromNumber, 
                    "Üzgünüm, bir hata oluştu. Lütfen tekrar deneyin veya doğrudan arayın.", 
                    tenantService.findTenantIdByWhatsAppNumber(businessPhoneNumber));
            } catch (Exception ex) {
                logger.error("Hata mesajı gönderilemedi", ex);
            }
        }
    }
    
    private void processMessage(BotSession session, String messageText) {
        // Büyük/küçük harf duyarsız kontrol
        String lowerMessage = messageText.toLowerCase();
        
        // Komut kontrolü
        if (lowerMessage.contains("randevu") || lowerMessage.contains("appointment") || 
            lowerMessage.equals("1") || session.getState() == BotState.AWAITING_SERVICE) {
            handleAppointmentFlow(session, messageText);
        } else if (lowerMessage.contains("iptal") || lowerMessage.contains("cancel")) {
            handleCancellation(session);
        } else if (lowerMessage.contains("hizmet") || lowerMessage.contains("service") || 
                  lowerMessage.contains("fiyat") || lowerMessage.contains("price")) {
            handleServiceInquiry(session);
        } else if (lowerMessage.contains("merhaba") || lowerMessage.contains("selam") || 
                  lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
            handleGreeting(session);
        } else {
            // Session state'e göre işlem
            handleStateBasedMessage(session, messageText);
        }
    }
    
    private void handleGreeting(BotSession session) {
        String welcomeMessage = String.format(
            "Merhaba! 👋 Randevu sistemimize hoş geldiniz.\n\n" +
            "Size nasıl yardımcı olabilirim?\n\n" +
            "1️⃣ Randevu almak için 'randevu' yazın\n" +
            "2️⃣ Hizmetlerimizi görmek için 'hizmetler' yazın\n" +
            "3️⃣ Randevu iptal için 'iptal' yazın\n\n" +
            "Herhangi bir sorunuz varsa doğrudan mesaj atabilirsiniz! 😊"
        );
        
        sendMessage(session, welcomeMessage);
    }
    
    private void handleServiceInquiry(BotSession session) {
        String servicesText = serviceService.getServicesForWhatsApp(session.getTenantId());
        sendMessage(session, servicesText);
    }
    
    private void handleAppointmentFlow(BotSession session, String messageText) {
        switch (session.getState()) {
            case INITIAL:
                startAppointmentFlow(session);
                break;
            case AWAITING_SERVICE:
                handleServiceSelection(session, messageText);
                break;
            case AWAITING_DATE:
                handleDateSelection(session, messageText);
                break;
            case AWAITING_TIME:
                handleTimeSelection(session, messageText);
                break;
            case AWAITING_CONFIRMATION:
                handleConfirmation(session, messageText);
                break;
            default:
                startAppointmentFlow(session);
                break;
        }
    }
    
    private void startAppointmentFlow(BotSession session) {
        // Müşteriyi bul veya oluştur
        CustomerDto customer = customerService.findByPhoneNumber(session.getPhoneNumber(), session.getTenantId())
                .orElse(null);
        
        if (customer == null) {
            // Yeni müşteri - isim sor
            session.setState(BotState.AWAITING_NAME);
            sendMessage(session, "Randevu almak için önce adınızı öğrenebilir miyim? 😊");
            return;
        }
        
        session.setCustomerId(customer.getId());
        
        // Hizmet seçimi
        String servicesText = serviceService.getServicesForWhatsApp(session.getTenantId());
        sendMessage(session, servicesText + "\n\nHangi hizmeti istiyorsunuz? Numarasını yazın:");
        session.setState(BotState.AWAITING_SERVICE);
    }
    
    private void handleServiceSelection(BotSession session, String messageText) {
        try {
            int serviceIndex = Integer.parseInt(messageText.trim()) - 1;
            List<ServiceDto> services = serviceService.findAllByTenant(session.getTenantId());
            
            if (serviceIndex >= 0 && serviceIndex < services.size()) {
                ServiceDto selectedService = services.get(serviceIndex);
                session.setServiceId(selectedService.getId());
                
                sendMessage(session, String.format(
                    "✅ *%s* hizmeti seçildi.\n\n" +
                    "Hangi tarih için randevu istiyorsunuz?\n" +
                    "Bugün için: 'bugün'\n" +
                    "Yarın için: 'yarın'\n" +
                    "Başka tarih için: GG.AA.YYYY (örn: 25.12.2024)",
                    selectedService.getName()
                ));
                
                session.setState(BotState.AWAITING_DATE);
            } else {
                sendMessage(session, "❌ Geçersiz hizmet numarası. Lütfen listeden bir numara seçin:");
            }
        } catch (NumberFormatException e) {
            sendMessage(session, "❌ Lütfen sadece hizmet numarasını yazın (örn: 1, 2, 3):");
        }
    }
    
    private void handleDateSelection(BotSession session, String messageText) {
        LocalDate selectedDate = null;
        String lowerMessage = messageText.toLowerCase().trim();
        
        if (lowerMessage.equals("bugün")) {
            selectedDate = LocalDate.now();
        } else if (lowerMessage.equals("yarın")) {
            selectedDate = LocalDate.now().plusDays(1);
        } else {
            // GG.AA.YYYY formatı
            try {
                selectedDate = LocalDate.parse(messageText.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            } catch (DateTimeParseException e) {
                sendMessage(session, "❌ Geçersiz tarih formatı. Lütfen GG.AA.YYYY formatında yazın (örn: 25.12.2024):");
                return;
            }
        }
        
        // Geçmiş tarih kontrolü
        if (selectedDate.isBefore(LocalDate.now())) {
            sendMessage(session, "❌ Geçmiş tarih seçilemez. Lütfen bugün veya gelecek bir tarih seçin:");
            return;
        }
        
        session.setSelectedDate(selectedDate);
        
        // Müsait saatleri göster
        String slotsText = slotService.getAvailableSlotsForWhatsApp(
                session.getTenantId(), session.getServiceId(), selectedDate);
        
        sendMessage(session, slotsText + "\n\nHangi saati istiyorsunuz? Numarasını yazın:");
        session.setState(BotState.AWAITING_TIME);
    }
    
    private void handleTimeSelection(BotSession session, String messageText) {
        try {
            int slotIndex = Integer.parseInt(messageText.trim()) - 1;
            SlotResponse slots = slotService.getAvailableSlots(
                    session.getTenantId(), session.getServiceId(), session.getSelectedDate());
            
            List<SlotResponse.TimeSlot> availableSlots = slots.getAvailableSlots()
                    .stream()
                    .filter(SlotResponse.TimeSlot::isAvailable)
                    .toList();
            
            if (slotIndex >= 0 && slotIndex < availableSlots.size()) {
                SlotResponse.TimeSlot selectedSlot = availableSlots.get(slotIndex);
                session.setSelectedTime(selectedSlot.getStartTime());
                
                // Onay mesajı
                ServiceDto service = serviceService.findById(session.getServiceId(), session.getTenantId()).orElse(null);
                String confirmationMessage = String.format(
                    "📋 *Randevu Özeti:*\n\n" +
                    "🔸 *Hizmet:* %s\n" +
                    "📅 *Tarih:* %s\n" +
                    "⏰ *Saat:* %s - %s\n" +
                    "💰 *Fiyat:* %s\n\n" +
                    "Randevuyu onaylamak için 'evet' yazın.\n" +
                    "İptal için 'hayır' yazın.",
                    service != null ? service.getName() : "Bilinmiyor",
                    session.getSelectedDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    selectedSlot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    selectedSlot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    service != null ? service.getFormattedPrice() : "Belirtilmemiş"
                );
                
                sendMessage(session, confirmationMessage);
                session.setState(BotState.AWAITING_CONFIRMATION);
            } else {
                sendMessage(session, "❌ Geçersiz saat numarası. Lütfen listeden bir numara seçin:");
            }
        } catch (NumberFormatException e) {
            sendMessage(session, "❌ Lütfen sadece saat numarasını yazın (örn: 1, 2, 3):");
        }
    }
    
    private void handleConfirmation(BotSession session, String messageText) {
        String lowerMessage = messageText.toLowerCase().trim();
        
        if (lowerMessage.equals("evet") || lowerMessage.equals("e") || lowerMessage.equals("yes")) {
            // Randevu oluştur
            try {
                CreateAppointmentRequest request = new CreateAppointmentRequest();
                request.setCustomerId(session.getCustomerId());
                request.setServiceId(session.getServiceId());
                request.setStartTime(session.getSelectedTime());
                
                appointmentService.createAppointment(request, session.getTenantId());
                
                sendMessage(session, "✅ Randevunuz başarıyla oluşturuldu! Zamanında bekleriz. 😊");
                
                // Session'ı temizle
                session.reset();
                
            } catch (Exception e) {
                logger.error("Randevu oluşturulamadı", e);
                sendMessage(session, "❌ Randevu oluşturulurken bir hata oluştu. Lütfen tekrar deneyin.");
                session.reset();
            }
        } else if (lowerMessage.equals("hayır") || lowerMessage.equals("h") || lowerMessage.equals("no")) {
            sendMessage(session, "Randevu iptal edildi. Başka bir zamana randevu almak için 'randevu' yazabilirsiniz.");
            session.reset();
        } else {
            sendMessage(session, "Lütfen 'evet' veya 'hayır' yazın:");
        }
    }
    
    private void handleCancellation(BotSession session) {
        // TODO: Randevu iptal etme işlemi
        sendMessage(session, "Randevu iptali için lütfen doğrudan arayın veya mağazaya gelin.");
    }
    
    private void handleStateBasedMessage(BotSession session, String messageText) {
        if (session.getState() == BotState.AWAITING_NAME) {
            // İsim alındı, müşteri oluştur
            try {
                CustomerDto customer = customerService.createCustomerFromWhatsApp(
                        messageText.trim(), session.getPhoneNumber(), session.getTenantId());
                session.setCustomerId(customer.getId());
                
                sendMessage(session, String.format("Merhaba %s! 👋", customer.getName()));
                
                // Hizmet seçimine geç
                String servicesText = serviceService.getServicesForWhatsApp(session.getTenantId());
                sendMessage(session, servicesText + "\n\nHangi hizmeti istiyorsunuz? Numarasını yazın:");
                session.setState(BotState.AWAITING_SERVICE);
                
            } catch (Exception e) {
                logger.error("Müşteri oluşturulamadı", e);
                sendMessage(session, "Bir hata oluştu. Lütfen tekrar deneyin.");
                session.reset();
            }
        } else {
            // Genel yardım mesajı
            handleGreeting(session);
        }
    }
    
    private void sendMessage(BotSession session, String message) {
        whatsAppService.sendMessage(session.getPhoneNumber(), message, session.getTenantId());
    }
    
    // Bot session management
    
    private enum BotState {
        INITIAL,
        AWAITING_NAME,
        AWAITING_SERVICE,
        AWAITING_DATE,
        AWAITING_TIME,
        AWAITING_CONFIRMATION
    }
    
    private static class BotSession {
        private String phoneNumber;
        private Long tenantId;
        private BotState state = BotState.INITIAL;
        private Long customerId;
        private Long serviceId;
        private LocalDate selectedDate;
        private LocalDateTime selectedTime;
        
        public BotSession(String phoneNumber, Long tenantId) {
            this.phoneNumber = phoneNumber;
            this.tenantId = tenantId;
        }
        
        public void reset() {
            this.state = BotState.INITIAL;
            this.customerId = null;
            this.serviceId = null;
            this.selectedDate = null;
            this.selectedTime = null;
        }
        
        // Getters and setters
        public String getPhoneNumber() { return phoneNumber; }
        public Long getTenantId() { return tenantId; }
        public BotState getState() { return state; }
        public void setState(BotState state) { this.state = state; }
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public Long getServiceId() { return serviceId; }
        public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
        public LocalDate getSelectedDate() { return selectedDate; }
        public void setSelectedDate(LocalDate selectedDate) { this.selectedDate = selectedDate; }
        public LocalDateTime getSelectedTime() { return selectedTime; }
        public void setSelectedTime(LocalDateTime selectedTime) { this.selectedTime = selectedTime; }
    }
}
