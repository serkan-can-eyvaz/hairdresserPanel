package com.example.barber.automation.service;

import com.example.barber.automation.dto.*;
import com.example.barber.automation.entity.Customer;
import com.example.barber.automation.entity.Tenant;
import com.example.barber.automation.entity.TenantUser;
import com.example.barber.automation.repository.CustomerRepository;
import com.example.barber.automation.repository.ServiceRepository;
import com.example.barber.automation.repository.TenantRepository;
import com.example.barber.automation.repository.TenantUserRepository;
import com.example.barber.automation.scheduler.AppointmentScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WhatsApp Bot servisi - AI Agent ile entegre
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
    private final TwilioSendService twilioSendService;
    private final AiAgentClient aiAgentClient;
    private final RestTemplate restTemplate;
    
    // Bot state management (basit in-memory cache)
    private final Map<String, BotSession> userSessions = new HashMap<>();
    
    @Autowired
    public WhatsAppBotService(TenantService tenantService,
                             CustomerService customerService,
                             ServiceService serviceService,
                             SlotService slotService,
                             AppointmentService appointmentService,
                             WhatsAppService whatsAppService,
                             TwilioSendService twilioSendService,
                             AiAgentClient aiAgentClient,
                             RestTemplate restTemplate) {
        this.tenantService = tenantService;
        this.customerService = customerService;
        this.serviceService = serviceService;
        this.slotService = slotService;
        this.appointmentService = appointmentService;
        this.whatsAppService = whatsAppService;
        this.aiAgentClient = aiAgentClient;
        this.twilioSendService = twilioSendService;
        this.restTemplate = restTemplate;
    }
    
    /**
     * Raw body'den gelen WhatsApp mesajını işle
     */
    public void processIncomingMessage(String rawBody) {
        try {
            logger.info("Raw body işleniyor: {}", rawBody);
            
            // JSON veya Twilio formatını parse et
            String fromNumber = extractFromNumber(rawBody);
            String messageText = extractMessageText(rawBody);
            
            if (fromNumber != null && messageText != null) {
                logger.info("Mesaj parse edildi - From: {}, Text: {}", fromNumber, messageText);
                
                // Bot session'ı bul veya oluştur
                BotSession session = getOrCreateSession(fromNumber, 1L); // Varsayılan tenant ID
                
                // Mesajı işle
                processMessage(session, messageText);
            } else {
                logger.warn("Mesaj parse edilemedi - From: {}, Text: {}", fromNumber, messageText);
            }
        } catch (Exception e) {
            logger.error("Raw body işlenirken hata oluştu", e);
        }
    }
    
    /**
     * Gelen WhatsApp mesajını işle
     */
    public void processIncomingMessage(WhatsAppWebhookRequest request) {
        try {
            logger.info("WhatsApp webhook mesajı alındı: {}", request.getObject());
            
            if (request.getEntry() != null && !request.getEntry().isEmpty()) {
                var entry = request.getEntry().get(0);
                if (entry.getChanges() != null && !entry.getChanges().isEmpty()) {
                    var change = entry.getChanges().get(0);
                    if (change.getValue() != null && change.getValue().getMessages() != null) {
                        var message = change.getValue().getMessages().get(0);
                        
                        String fromNumber = message.getFrom();
                        String messageType = message.getType();
                        String messageText = message.getText() != null ? message.getText().getBody() : "";
                        
                        logger.info("Gelen mesaj - From: {}, Type: {}, Text: {}", fromNumber, messageType, messageText);
                        
                        // Müşteri herhangi bir numaradan yazabilir, varsayılan kuaförü kullan
                        Tenant defaultTenant = getDefaultTenant();
                        if (defaultTenant == null) {
                            logger.error("Sistemde hiç aktif kuaför yok");
                            return;
                        }
                        
                        // Session'ı al veya oluştur (müşteri numarası + varsayılan kuaför)
                        BotSession session = getOrCreateSession(fromNumber, defaultTenant.getId());
                        if (session == null) {
                            logger.error("Session oluşturulamadı - Phone: {}", fromNumber);
                return;
            }
            
                        // Son mesajı session'a kaydet
                        session.setLastMessage(messageText);
                        
                        // AI Agent ile akıllı mesaj işleme
                        String aiResponse = processWithAI(session, messageText, defaultTenant);
                        
                        if (aiResponse != null) {
                            logger.info("AI Agent yanıt verdi: {}", aiResponse);
                            // AI yanıt verirse, Twilio üzerinden gönder
                            try {
                                String to = "+".concat(fromNumber.startsWith("+") ? fromNumber.substring(1) : fromNumber);
                                twilioSendService.sendWhatsAppText("whatsapp:" + to, aiResponse);
                            } catch (Exception e) {
                                logger.error("Twilio üzerinden mesaj gönderilemedi", e);
                            }
                        } else {
                            logger.info("AI Agent yanıt vermedi");
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("WhatsApp webhook mesajı işlenirken hata oluştu", e);
        }
    }
    
    /**
     * AI Agent ile akıllı mesaj işleme
     */
    private String processWithAI(BotSession session, String messageText, Tenant tenant) {
        try {
            logger.info("=== AI AGENT ÇAĞRISI BAŞLADI ===");
            logger.info("Session: phone={}, state={}, tenant={}", 
                       session.getPhoneNumber(), session.getState(), session.getTenantId());
            logger.info("Message: {}", messageText);
            
            // AI Agent'a sadece mesajı gönder, session management AI Agent'da olsun
            com.example.barber.automation.dto.AgentRespondRequest req = new com.example.barber.automation.dto.AgentRespondRequest();
            req.setTenant_id(session.getTenantId());
            req.setFrom_number(session.getPhoneNumber());
            req.setMessage(messageText);
            
            logger.info("AI Agent'a gönderilen request: {}", req);
            
            var agentResp = aiAgentClient.respond(req);
            logger.info("AI-Agent raw response: {}", agentResp);
            
            if (agentResp != null) {
                logger.info("AI Agent response details:");
                logger.info("  - ok: {}", agentResp.isOk());
                logger.info("  - intent: {}", agentResp.getIntent());
                logger.info("  - reply: {}", agentResp.getReply());
                logger.info("  - nextState: {}", agentResp.getNextState());
                logger.info("  - extractedInfo: {}", agentResp.getExtractedInfo());
                
                if (agentResp.isOk() && agentResp.getReply() != null) {
                    logger.info("AI Agent yanıt verdi: {}", agentResp.getReply());
                    
                    // AI Agent response'ını işle ve database'e kaydet
                    processAIResponse(session, agentResp, tenant);
                    
                    logger.info("=== AI AGENT ÇAĞRISI TAMAMLANDI ===");
                    
                    // Konum önerisi varsa AI yanıtına ekle
                    String finalResponse = agentResp.getReply();
                    if (session.getLocationSuggestion() != null && !session.getLocationSuggestion().isEmpty()) {
                        finalResponse += "\n\n" + session.getLocationSuggestion();
                        logger.info("Konum önerisi eklendi: {}", session.getLocationSuggestion());
                    }
                    
                    // Kuaför listesi varsa AI yanıtına ekle
                    if (session.getBarberList() != null && !session.getBarberList().isEmpty()) {
                        finalResponse = session.getBarberList(); // Kuaför listesi varsa onu göster
                        logger.info("Kuaför listesi gösteriliyor");
                    }
                    
                    return finalResponse;
                } else {
                    logger.info("AI Agent yanıtı geçersiz");
                }
            } else {
                logger.info("AI Agent null yanıt döndü");
            }
            
        } catch (Exception e) {
            logger.error("AI-Agent yanıtı işlenirken hata oluştu", e);
        }
        
        logger.info("=== AI AGENT ÇAĞRISI BAŞARISIZ ===");
        return null; // AI yanıt vermezse null döndür
    }
    
    /**
     * Session state'e göre AI prompt'u oluştur
     */
    private String createAIPrompt(BotSession session, String messageText) {
        StringBuilder prompt = new StringBuilder();
        
        // Context bilgisi
        prompt.append("Sen bir kuaför salonu randevu botusun. Müşteri ile konuşuyorsun.\n\n");
        
        // Session state bilgisi
        prompt.append("MEVCUT DURUM:\n");
        prompt.append("- Session State: ").append(session.getState()).append("\n");
        prompt.append("- Müşteri ID: ").append(session.getCustomerId() != null ? session.getCustomerId() : "Henüz oluşturulmadı").append("\n");
        prompt.append("- Hizmet ID: ").append(session.getServiceId() != null ? session.getServiceId() : "Henüz seçilmedi").append("\n");
        prompt.append("- Seçilen Tarih: ").append(session.getSelectedDate() != null ? session.getSelectedDate() : "Henüz seçilmedi").append("\n");
        prompt.append("- Seçilen Saat: ").append(session.getSelectedTime() != null ? session.getSelectedTime() : "Henüz seçilmedi").append("\n\n");
        
        // Müşteri mesajı
        prompt.append("MÜŞTERİ MESAJI: ").append(messageText).append("\n\n");
        
        // Görev talimatları
        prompt.append("GÖREV:\n");
        prompt.append("1. Müşteri mesajını analiz et\n");
        prompt.append("2. Session state'e göre uygun yanıt ver\n");
        prompt.append("3. Gerekirse session state'i güncelle\n\n");
        
        // State'e göre özel talimatlar
        switch (session.getState()) {
            case INITIAL:
                prompt.append("INITIAL STATE - İlk karşılama:\n");
                prompt.append("- Eğer 'merhaba', 'selam' gibi selamlaşma varsa: Karşıla ve randevu seçeneklerini sun\n");
                prompt.append("- Eğer 'randevu' kelimesi varsa: Konum sormaya geç (AWAITING_LOCATION)\n");
                prompt.append("- Eğer isim verilmişse: Müşteri oluştur ve hizmet seçimine geç\n");
                break;
                
            case AWAITING_NAME:
                prompt.append("AWAITING_NAME STATE - İsim bekleniyor:\n");
                prompt.append("- Müşteri ismini aldığını onayla\n");
                prompt.append("- Hizmet seçimine geç\n");
                prompt.append("- Session state'i AWAITING_SERVICE yap\n");
                break;
                
            case AWAITING_SERVICE:
                prompt.append("AWAITING_SERVICE STATE - Hizmet seçimi bekleniyor:\n");
                prompt.append("- Hizmet numarasını (1, 2, 3...) al\n");
                prompt.append("- Hizmet seçildiğini onayla\n");
                prompt.append("- Tarih seçimine geç\n");
                prompt.append("- Session state'i AWAITING_DATE yap\n");
                break;
                
            case AWAITING_LOCATION:
                prompt.append("AWAITING_LOCATION STATE - Konum bekleniyor:\n");
                prompt.append("- İl ve ilçe bilgisini al (örn: Ankara Çankaya)\n");
                prompt.append("- O bölgedeki kuaförleri listele\n");
                prompt.append("- Kuaför seçimine geç\n");
                prompt.append("- Session state'i AWAITING_BARBER yap\n");
                break;
                
            case AWAITING_BARBER:
                prompt.append("AWAITING_BARBER STATE - Kuaför seçimi bekleniyor:\n");
                prompt.append("- Kuaför numarasını (1, 2, 3...) al\n");
                prompt.append("- Kuaför seçildiğini onayla\n");
                prompt.append("- İsim sormaya geç\n");
                prompt.append("- Session state'i AWAITING_NAME yap\n");
                break;
                
            case AWAITING_DATE:
                prompt.append("AWAITING_DATE STATE - Tarih bekleniyor:\n");
                prompt.append("- Tarihi (bugün, yarın, GG.AA.YYYY) al\n");
                prompt.append("- Tarih seçildiğini onayla\n");
                prompt.append("- Saat seçimine geç\n");
                prompt.append("- Session state'i AWAITING_TIME yap\n");
                break;
                
            case AWAITING_TIME:
                prompt.append("AWAITING_TIME STATE - Saat bekleniyor:\n");
                prompt.append("- Saat numarasını (1, 2, 3...) al\n");
                prompt.append("- Saat seçildiğini onayla\n");
                prompt.append("- Onay mesajı göster\n");
                prompt.append("- Session state'i AWAITING_CONFIRMATION yap\n");
                break;
                
            case AWAITING_CONFIRMATION:
                prompt.append("AWAITING_CONFIRMATION STATE - Onay bekleniyor:\n");
                prompt.append("- 'evet'/'hayır' yanıtını al\n");
                prompt.append("- Onaylanırsa randevuyu kaydet\n");
                prompt.append("- Session'ı reset et\n");
                break;
        }
        
        prompt.append("\nYANIT FORMATI:\n");
        prompt.append("- Doğrudan müşteriye söylenecek mesajı yaz\n");
        prompt.append("- Emoji kullan (😊, ✅, 📅, ⏰, 💰)\n");
        prompt.append("- Kısa ve net ol\n");
        prompt.append("- Türkçe yaz\n");
        
        return prompt.toString();
    }
    
    /**
     * AI Agent yanıtını işler ve gerekli database işlemlerini yapar.
     */
    private void processAIResponse(BotSession session, com.example.barber.automation.dto.AgentRespondResponse aiResponse, Tenant tenant) {
        // TODO: Implement database saving logic here
        logger.info("AI Agent yanıtı işleniyor: Intent={}, NextState={}, ExtractedInfo={}", 
                   aiResponse.getIntent(), aiResponse.getNextState(), aiResponse.getExtractedInfo());
        
        // Session state'i AI Agent'ın önerdiği nextState ile güncelle
        if (aiResponse.getNextState() != null) {
            try {
                String nextState = aiResponse.getNextState().toUpperCase();
                logger.info("AI Agent'tan gelen nextState: '{}' -> '{}'", aiResponse.getNextState(), nextState);
                // AI Agent'tan gelen state'i BotState enum'una çevir
                switch (nextState) {
                    case "AWAITING_LOCATION":
                        session.setState(BotState.AWAITING_LOCATION);
                        break;
                    case "AWAITING_BARBER_SELECTION":
                        session.setState(BotState.AWAITING_BARBER_SELECTION);
                        break;
                    case "AWAITING_BARBER":
                        session.setState(BotState.AWAITING_BARBER);
                        break;
                    case "AWAITING_NAME":
                        session.setState(BotState.AWAITING_NAME);
                        break;
                    case "AWAITING_SERVICE":
                        session.setState(BotState.AWAITING_SERVICE);
                        break;
                    case "AWAITING_DATE":
                        session.setState(BotState.AWAITING_DATE);
                        break;
                    case "AWAITING_TIME":
                        session.setState(BotState.AWAITING_TIME);
                        break;
                    case "AWAITING_CONFIRMATION":
                        session.setState(BotState.AWAITING_CONFIRMATION);
                        break;
                    case "COMPLETED":
                        session.setState(BotState.COMPLETED);
                        break;
                    default:
                        logger.warn("AI Agent'tan geçersiz nextState geldi: {}", aiResponse.getNextState());
                        // Türkçe karakterleri İngilizce'ye çevir ve tekrar dene
                        String normalizedState = aiResponse.getNextState().toUpperCase()
                            .replace("İ", "I").replace("Ğ", "G").replace("Ü", "U")
                            .replace("Ş", "S").replace("Ö", "O").replace("Ç", "C");
                        
                        if (normalizedState.equals("AWAİTİNG_BARBER_SELECTİON")) {
                            session.setState(BotState.AWAITING_BARBER_SELECTION);
                        } else if (normalizedState.equals("AWAİTİNG_SERVİCE")) {
                            session.setState(BotState.AWAITING_SERVICE);
                        } else if (normalizedState.equals("AWAİTİNG_DATE")) {
                            session.setState(BotState.AWAITING_DATE);
                        } else if (normalizedState.equals("AWAİTİNG_TİME")) {
                            session.setState(BotState.AWAITING_TIME);
                        } else if (normalizedState.equals("AWAİTİNG_CONFİRMATİON")) {
                            session.setState(BotState.AWAITING_CONFIRMATION);
                        } else {
                            // Geçersiz state gelirse varsayılan olarak INITIAL'a çevir
                            session.setState(BotState.INITIAL);
                        }
                }
            } catch (Exception e) {
                logger.warn("Session state güncellenirken hata: {}", e.getMessage());
                session.setState(BotState.INITIAL);
            }
        }
        
        // Extracted info'yu session'a kaydet
        Map<String, Object> extractedInfo = aiResponse.getExtractedInfo();
        if (extractedInfo != null) {
            if (extractedInfo.containsKey("customer_name")) {
                String customerName = (String) extractedInfo.get("customer_name");
                // Telefon numarası formatını düzelt
                String formattedPhone = session.getPhoneNumber();
                if (!formattedPhone.startsWith("+")) {
                    formattedPhone = "+" + formattedPhone;
                }
                
                logger.info("Creating customer with name: {}, phone: {} (formatted: {})", 
                           customerName, session.getPhoneNumber(), formattedPhone);
                
                // Müşteriyi bul veya oluştur
                CustomerDto customer = customerService.createCustomerFromWhatsApp(
                        customerName, formattedPhone, session.getTenantId());
                session.setCustomerId(customer.getId());
                logger.info("Müşteri kaydedildi/bulundu: ID={}, Name={}", customer.getId(), customerName);
            }
            if (extractedInfo.containsKey("service_preference")) {
                session.setSelectedService((String) extractedInfo.get("service_preference"));
                logger.info("Hizmet seçildi: {}", session.getSelectedService());
            }
            
            // Kuaför seçimi
            if (extractedInfo.containsKey("barber_selection")) {
                try {
                    String barberSelection = (String) extractedInfo.get("barber_selection");
                    int barberIndex = Integer.parseInt(barberSelection) - 1;
                    
                    if (session.getAvailableBarbers() != null && barberIndex >= 0 && barberIndex < session.getAvailableBarbers().size()) {
                        TenantDto selectedBarber = session.getAvailableBarbers().get(barberIndex);
                        session.setSelectedTenantId(selectedBarber.getId());
                        session.setState(BotState.AWAITING_NAME);
                        logger.info("Kuaför seçildi: ID={}, Name={}", selectedBarber.getId(), selectedBarber.getName());
                        
                        // Kuaför seçimi onay mesajı gönder
                        sendMessage(session, 
                            "✅ " + selectedBarber.getName() + " seçildi! 💇‍♂️\n\n" +
                            "Şimdi adınızı öğrenebilir miyim? 😊"
                        );
                        
                        // AI Agent'ın yanıtını gönderme (çünkü kuaför seçimi mesajı zaten gönderildi)
                        return;
                    } else {
                        logger.warn("Geçersiz kuaför seçimi: {}", barberSelection);
                        sendMessage(session, "❌ Geçersiz kuaför numarası. Lütfen listeden bir numara seçin.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Kuaför seçimi sayısal değil: {}", extractedInfo.get("barber_selection"));
                    sendMessage(session, "❌ Lütfen sadece numara yazın.");
                    return;
                }
            }
            if (extractedInfo.containsKey("location_preference")) {
                String location = (String) extractedInfo.get("location_preference");
                session.setSelectedLocation(location);
                logger.info("Konum seçildi: {}", session.getSelectedLocation());
                
                // Konum bazlı kuaförleri listele (mesaj gönderme işlemi burada yapılacak)
                listBarbersByLocation(location, session);
                
                // AI Agent'ın yanıtını gönderme (çünkü listBarbersByLocation zaten mesaj gönderiyor)
                return;
            }
            if (extractedInfo.containsKey("date_preference")) {
                try {
                    String dateStr = (String) extractedInfo.get("date_preference");
                    LocalDate date = null;
                    
                    // Farklı tarih formatlarını dene
                    try {
                        // "1 Eylül 2025" formatı
                        if (dateStr.contains("Eylül")) {
                            dateStr = dateStr.replace("Eylül", "09");
                        } else if (dateStr.contains("Ocak")) {
                            dateStr = dateStr.replace("Ocak", "01");
                        } else if (dateStr.contains("Şubat")) {
                            dateStr = dateStr.replace("Şubat", "02");
                        } else if (dateStr.contains("Mart")) {
                            dateStr = dateStr.replace("Mart", "03");
                        } else if (dateStr.contains("Nisan")) {
                            dateStr = dateStr.replace("Nisan", "04");
                        } else if (dateStr.contains("Mayıs")) {
                            dateStr = dateStr.replace("Mayıs", "05");
                        } else if (dateStr.contains("Haziran")) {
                            dateStr = dateStr.replace("Haziran", "06");
                        } else if (dateStr.contains("Temmuz")) {
                            dateStr = dateStr.replace("Temmuz", "07");
                        } else if (dateStr.contains("Ağustos")) {
                            dateStr = dateStr.replace("Ağustos", "08");
                        } else if (dateStr.contains("Ekim")) {
                            dateStr = dateStr.replace("Ekim", "10");
                        } else if (dateStr.contains("Kasım")) {
                            dateStr = dateStr.replace("Kasım", "11");
                        } else if (dateStr.contains("Aralık")) {
                            dateStr = dateStr.replace("Aralık", "12");
                        }
                        
                        // "1 09 2025" formatını "01.09.2025" formatına çevir
                        String[] parts = dateStr.trim().split("\\s+");
                        if (parts.length == 3) {
                            String day = String.format("%02d", Integer.parseInt(parts[0]));
                            String month = String.format("%02d", Integer.parseInt(parts[1]));
                            String year = parts[2];
                            dateStr = day + "." + month + "." + year;
                        }
                        
                        date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                    } catch (Exception e1) {
                        // "01.09.2025" formatını dene
                        try {
                            date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                        } catch (Exception e2) {
                            // "2025-09-01" formatını dene
                            date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        }
                    }
                    
                    session.setSelectedDate(date);
                    logger.info("Tarih seçildi: {}", session.getSelectedDate());
                } catch (DateTimeParseException e) {
                    logger.warn("Geçersiz tarih formatı: {}", extractedInfo.get("date_preference"));
                }
            }
            if (extractedInfo.containsKey("time_preference")) {
                try {
                    LocalTime time = LocalTime.parse((String) extractedInfo.get("time_preference"), DateTimeFormatter.ofPattern("HH:mm"));
                    // selectedDate null kontrolü ekle
                    if (session.getSelectedDate() != null) {
                        session.setSelectedTime(session.getSelectedDate().atTime(time));
                        logger.info("Saat seçildi: {}", session.getSelectedTime());
                    } else {
                        logger.warn("Tarih seçilmediği için saat kaydedilemedi");
                    }
                } catch (DateTimeParseException e) {
                    logger.warn("Geçersiz saat formatı: {}", extractedInfo.get("time_preference"));
                }
            }
        }
        
        // Randevu onaylandığında database'e kaydet
        if (aiResponse.getIntent().equalsIgnoreCase("confirm_appointment") && session.getCustomerId() != null && session.getSelectedService() != null && session.getSelectedDate() != null && session.getSelectedTime() != null) {
            // TODO: Randevu oluşturma ve kaydetme
            logger.info("Randevu onaylandı, database'e kaydedilecek: Müşteri={}, Hizmet={}, Tarih={}, Saat={}",
                       session.getCustomerId(), session.getSelectedService(), session.getSelectedDate(), session.getSelectedTime());
            session.reset(); // Randevu tamamlandıktan sonra session'ı sıfırla
        }
    }

    /**
     * AI yanıtından session state'i güncelle
     */
    private void updateSessionFromAI(BotSession session, com.example.barber.automation.dto.AgentRespondResponse aiResponse) {
        // AI yanıtından session state'i otomatik güncelle
        String intent = aiResponse.getIntent();
        String reply = aiResponse.getReply();
        
        logger.info("AI Agent intent: {}, reply: {}", intent, reply);
        logger.info("Session state before update: {}", session.getState());
        
        if (intent != null) {
            switch (intent.toLowerCase()) {
                case "greeting":
                    // Karşılama - session state'i değiştirme
                    logger.info("Greeting intent - session state unchanged");
                    break;
                    
                case "appointment_start":
                    // Randevu başlatma - isim sormaya geç
                    logger.info("Appointment start intent - changing state to AWAITING_NAME");
                    session.setState(BotState.AWAITING_NAME);
                    break;
                    
                case "provide_name":
                    // İsim verildi - müşteri oluştur ve hizmet seçimine geç
                    logger.info("Provide name intent - creating customer and changing state to AWAITING_SERVICE");
                    try {
                        // Müşteri mesajından ismi çıkar (son mesaj)
                        String customerName = extractCustomerName(session.getPhoneNumber(), session.getTenantId(), session.getLastMessage());
                        if (customerName != null) {
                            // Telefon numarası formatını düzelt
                            String formattedPhone = session.getPhoneNumber();
                            if (!formattedPhone.startsWith("+")) {
                                formattedPhone = "+" + formattedPhone;
                            }
                            
                            logger.info("Creating customer with name: {}, phone: {} (formatted: {})", 
                                       customerName, session.getPhoneNumber(), formattedPhone);
                            
                            CustomerDto customer = customerService.createCustomerFromWhatsApp(
                                    customerName, formattedPhone, session.getTenantId());
                            session.setCustomerId(customer.getId());
                            session.setState(BotState.AWAITING_SERVICE);
                            logger.info("Customer created with ID: {}, state changed to AWAITING_SERVICE", customer.getId());
                        }
                    } catch (Exception e) {
                        logger.error("Müşteri oluşturulamadı", e);
                    }
                    break;
                    
                case "provide_service":
                    // Hizmet seçildi - tarih seçimine geç
                    logger.info("Provide service intent - changing state to AWAITING_DATE");
                    session.setState(BotState.AWAITING_DATE);
                    break;
                    
                case "provide_date":
                    // Tarih seçildi - saat seçimine geç
                    logger.info("Provide date intent - changing state to AWAITING_TIME");
                    session.setState(BotState.AWAITING_TIME);
                    break;
                    
                case "provide_time":
                    // Saat seçildi - onay beklemeye geç
                    logger.info("Provide time intent - changing state to AWAITING_CONFIRMATION");
                    session.setState(BotState.AWAITING_CONFIRMATION);
                    break;
                    
                case "confirm_appointment":
                    // Randevu onaylandı - session'ı reset et
                    logger.info("Confirm appointment intent - resetting session");
                    session.reset();
                    break;
                    
                default:
                    // Bilinmeyen intent - session state'i değiştirme
                    logger.info("Unknown intent: {} - session state unchanged", intent);
                    break;
            }
        }
        
        logger.info("Session state after update: {}", session.getState());
    }
    
    /**
     * Müşteri ismini çıkar (son mesajdan)
     */
    private String extractCustomerName(String phoneNumber, Long tenantId, String lastMessage) {
        // Telefon numarası formatını kontrol et
        String formattedPhone = phoneNumber;
        if (!phoneNumber.startsWith("+")) {
            formattedPhone = "+" + formattedPhone;
        }
        
        logger.info("Extracting customer name for phone: {} (formatted: {}), lastMessage: {}", phoneNumber, formattedPhone, lastMessage);
        
        // Son mesajdan ismi çıkar
        if (lastMessage != null && !lastMessage.trim().isEmpty()) {
            return lastMessage.trim();
        }
        
        return "Müşteri"; // Fallback
    }
    
    /**
     * Mesajı işle (AI Agent yanıt vermezse)
     */
    private void processMessage(BotSession session, String messageText) {
        logger.info("Manual flow ile mesaj işleniyor: session={}, message={}", session.getPhoneNumber(), messageText);
        
        // Manuel akış - AI Agent'a bağımlı olmadan
        processManualFlow(session, messageText);
    }
    
    private void processManualFlow(BotSession session, String messageText) {
        String lowerMessage = messageText.toLowerCase().trim();
        
        switch (session.getState()) {
            case INITIAL:
                // İlk mesaj - lokasyon sor
                sendMessage(session, "Merhaba! 👋 Randevu almak için önce hangi şehir veya ilçede hizmet almak istediğinizi belirtin:");
                session.setState(BotState.AWAITING_LOCATION);
                break;
                
            case AWAITING_LOCATION:
                // Lokasyon seçildi - kuaförleri listele
                session.setSelectedLocation(messageText);
                listBarbersByLocation(messageText, session);
                session.setState(BotState.AWAITING_BARBER_SELECTION);
                break;
                
            case AWAITING_BARBER_SELECTION:
                // Kuaför seçildi
                handleBarberSelection(session, messageText);
                break;
                
            case AWAITING_NAME:
                // İsim alındı
                handleNameInput(session, messageText);
                break;
                
            case AWAITING_SERVICE:
                // Hizmet seçildi
                handleServiceSelection(session, messageText);
                break;
                
            case AWAITING_DATE:
                // Tarih seçildi
                handleDateSelection(session, messageText);
                break;
                
            case AWAITING_TIME:
                // Saat seçildi
                handleTimeSelection(session, messageText);
                break;
                
            case AWAITING_CONFIRMATION:
                // Onay
                handleConfirmation(session, messageText);
                break;
                
            default:
                session.setState(BotState.INITIAL);
                processManualFlow(session, messageText);
                break;
        }
    }
    
    /**
     * Kuaför seçimini işle
     */
    private void handleBarberSelection(BotSession session, String messageText) {
        try {
            // Mesajdan kuaför numarasını çıkar
            int barberIndex = Integer.parseInt(messageText.trim()) - 1;
            
            if (barberIndex >= 0 && barberIndex < session.getAvailableBarbers().size()) {
                TenantDto selectedBarber = session.getAvailableBarbers().get(barberIndex);
                session.setSelectedBarberId(selectedBarber.getId());
                session.setState(BotState.AWAITING_NAME);
                
                sendMessage(session, 
                    "✅ " + selectedBarber.getName() + " seçildi! 💇‍♂️\n\n" +
                    "Şimdi adınızı öğrenebilir miyim? 😊"
                );
            } else {
                sendMessage(session, "❌ Geçersiz kuaför numarası. Lütfen listeden bir numara seçin.");
            }
        } catch (NumberFormatException e) {
            sendMessage(session, "❌ Lütfen sadece numara yazın.");
        }
    }
    
    /**
     * Konum bazlı kuaförleri listele
     */
    public void listBarbersByLocation(String location, BotSession session) {
        try {
            // Konum bilgisini parse et (şehir, ilçe)
            String[] locationParts = location.split(",");
            String city = locationParts[0].trim();
            String district = locationParts.length > 1 ? locationParts[1].trim() : null;
            
            // Kuaförleri getir
            List<TenantDto> barbers;
            if (district != null && !district.isEmpty()) {
                barbers = tenantService.findByCityAndDistrictDto(city, district);
        } else {
                barbers = tenantService.findByCityDto(city);
            }
            
            if (barbers.isEmpty()) {
                sendMessage(session, 
                    "😔 " + location + " bölgesinde aktif kuaför bulunamadı.\n\n" +
                    "Başka bir bölge deneyebilir misiniz? 🏠"
                );
                return;
            }
            
            // Kuaförleri listele
            StringBuilder barberList = new StringBuilder();
            barberList.append("🏠 ").append(location).append(" bölgesindeki kuaförlerimiz:\n\n");
            
            for (int i = 0; i < barbers.size(); i++) {
                TenantDto barber = barbers.get(i);
                barberList.append((i + 1)).append("️⃣ ").append(barber.getName()).append("\n");
                if (barber.getAddress() != null) {
                    barberList.append("📍 ").append(barber.getAddress()).append("\n");
                }
                barberList.append("\n");
            }
            
            barberList.append("Hangi kuaförü seçmek istiyorsunuz? Numarasını yazın:");
            
            // Session'a kuaförleri kaydet
            session.setAvailableBarbers(barbers);
            session.setState(BotState.AWAITING_BARBER_SELECTION);
            
            sendMessage(session, barberList.toString());
            
        } catch (Exception e) {
            logger.error("Konum bazlı kuaför listeleme hatası: {}", e.getMessage());
            sendMessage(session, "❌ Bir hata oluştu. Lütfen tekrar deneyin.");
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
    
    private void handleNameInput(BotSession session, String messageText) {
        String customerName = messageText.trim();
        
        if (customerName.length() < 2) {
            sendMessage(session, "❌ Lütfen geçerli bir isim girin:");
            return;
        }
        
        // Müşteri oluştur
        try {
            CustomerDto customer = customerService.createCustomerFromWhatsApp(
                customerName, session.getPhoneNumber(), session.getTenantId());
            
            session.setCustomerId(customer.getId());
            
            // Hizmet seçimi
            String servicesText = serviceService.getServicesForWhatsApp(session.getTenantId());
            sendMessage(session, 
                "Merhaba " + customerName + "! 😊\n\n" +
                servicesText + "\n\nHangi hizmeti istiyorsunuz? Numarasını yazın:");
            session.setState(BotState.AWAITING_SERVICE);
            
        } catch (Exception e) {
            logger.error("Müşteri oluşturma hatası: {}", e.getMessage());
            sendMessage(session, "❌ Bir hata oluştu. Lütfen tekrar deneyin.");
        }
    }
    
    private void handleServiceSelection(BotSession session, String messageText) {
        try {
            int serviceIndex = Integer.parseInt(messageText.trim()) - 1;
            List<ServiceDto> services = serviceService.findAllByTenant(session.getTenantId());
            
            if (serviceIndex >= 0 && serviceIndex < services.size()) {
                ServiceDto selectedService = services.get(serviceIndex);
                session.setServiceId(selectedService.getId());
                
                sendMessage(session, 
                    "✅ " + selectedService.getName() + " seçildi.\n\n" +
                    "Şimdi hangi tarihte randevu almak istiyorsunuz?\n" +
                    "Örnek: yarın, bugün, 15 Ağustos, vs.");
                
                session.setState(BotState.AWAITING_DATE);
            } else {
                sendMessage(session, "❌ Geçersiz hizmet numarası. Lütfen listeden bir numara seçin.");
            }
        } catch (NumberFormatException e) {
            sendMessage(session, "❌ Lütfen sadece numara yazın.");
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
        
        if (lowerMessage.equals("evet") || lowerMessage.equals("e") || lowerMessage.equals("yes") || 
            lowerMessage.contains("onaylıyorum") || lowerMessage.contains("onayla") || lowerMessage.contains("tamam")) {
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
    
    private String extractFromNumber(String rawBody) {
        try {
            // JSON formatını kontrol et
            if (rawBody.trim().startsWith("{")) {
                // JSON formatı
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(rawBody);
                String fromNumber = jsonNode.get("From").asText();
                // whatsapp: prefix'ini kaldır
                if (fromNumber.startsWith("whatsapp:")) {
                    fromNumber = fromNumber.substring(9);
                }
                return fromNumber;
            } else if (rawBody.contains("From=")) {
                // Twilio webhook formatı
                String fromPart = rawBody.substring(rawBody.indexOf("From=") + 5);
                String fromNumber = fromPart.split("&")[0];
                // whatsapp: prefix'ini kaldır
                if (fromNumber.startsWith("whatsapp:")) {
                    fromNumber = fromNumber.substring(9);
                }
                return fromNumber;
            }
        } catch (Exception e) {
            logger.error("From numarası çıkarılamadı", e);
        }
        return null;
    }
    
    private String extractMessageText(String rawBody) {
        try {
            // JSON formatını kontrol et
            if (rawBody.trim().startsWith("{")) {
                // JSON formatı
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(rawBody);
                return jsonNode.get("Body").asText();
            } else if (rawBody.contains("Body=")) {
                // Twilio webhook formatı
                String bodyPart = rawBody.substring(rawBody.indexOf("Body=") + 5);
                String messageText = bodyPart.split("&")[0];
                // URL decode
                return java.net.URLDecoder.decode(messageText, "UTF-8");
            }
        } catch (Exception e) {
            logger.error("Mesaj metni çıkarılamadı", e);
        }
        return null;
    }
    
    /**
     * Session'ı al veya oluştur
     */
    private BotSession getOrCreateSession(String fromNumber, Long tenantId) {
        String sessionKey = fromNumber + "_" + tenantId;
        
        logger.info("=== SESSION YÖNETİMİ ===");
        logger.info("Phone: {}, Tenant: {}, SessionKey: {}", fromNumber, tenantId, sessionKey);
        
        BotSession session = userSessions.computeIfAbsent(sessionKey, k -> {
            logger.info("YENİ SESSION OLUŞTURULDU: {}", sessionKey);
            BotSession newSession = new BotSession(fromNumber, tenantId);
            logger.info("Yeni session state: {}", newSession.getState());
            return newSession;
        });
        
        logger.info("Session bulundu: key={}, state={}, customerId={}", 
                   sessionKey, session.getState(), session.getCustomerId());
        logger.info("=== SESSION YÖNETİMİ TAMAMLANDI ===");
        
        return session;
    }
    
    /**
     * Varsayılan kuaförü al (sistemdeki ilk aktif kuaför)
     */
    private Tenant getDefaultTenant() {
        try {
            List<Tenant> activeTenants = tenantService.findByActiveTrue();
            if (!activeTenants.isEmpty()) {
                Tenant defaultTenant = activeTenants.get(0);
                logger.info("Varsayılan kuaför kullanılıyor: ID={}, Name={}, Phone={}", 
                           defaultTenant.getId(), defaultTenant.getName(), defaultTenant.getPhoneNumber());
                return defaultTenant;
            }
            
            logger.warn("Hiç aktif kuaför bulunamadı");
            return null;
            
        } catch (Exception e) {
            logger.error("Varsayılan kuaför alınırken hata oluştu", e);
            return null;
        }
    }

    /**
     * WhatsApp mesajı gönder
     */
    private void sendWhatsAppMessage(String toNumber, String message, Tenant tenant) {
        try {
            whatsAppService.sendMessage(toNumber, message, tenant.getId());
        } catch (Exception e) {
            logger.error("WhatsApp mesajı gönderilemedi", e);
        }
    }
    
    // Bot session management
    
    private enum BotState {
        INITIAL,
        AWAITING_LOCATION,
        AWAITING_BARBER_SELECTION,
        AWAITING_BARBER,
        AWAITING_NAME,
        AWAITING_SERVICE,
        AWAITING_DATE,
        AWAITING_TIME,
        AWAITING_CONFIRMATION,
        COMPLETED
    }
    
    private static class BotSession {
        private String phoneNumber;
        private Long tenantId;
        private BotState state = BotState.INITIAL;
        private Long customerId;
        private Long serviceId;
        private LocalDate selectedDate;
        private LocalDateTime selectedTime;
        private String lastMessage; // Son mesajı sakla
        private String selectedService; // Seçilen hizmet adı
        private String selectedLocation; // Seçilen konum
        private String locationSuggestion; // Konum önerisi
        private Long suggestedTenantId; // Önerilen kuaför ID'si
        private Long selectedTenantId; // Seçilen kuaför ID'si
        private Long selectedBarberId; // Seçilen kuaför ID'si (alias)
        private List<TenantDto> availableBarbers; // Mevcut kuaförler listesi
        private String barberList; // Kuaför listesi metni
        
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
            this.lastMessage = null;
            this.selectedService = null;
            this.selectedLocation = null;
            this.locationSuggestion = null;
            this.suggestedTenantId = null;
            this.selectedTenantId = null;
            this.selectedBarberId = null;
            this.availableBarbers = null;
            this.barberList = null;
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
        public String getLastMessage() { return lastMessage; }
        public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
        public String getSelectedService() { return selectedService; }
        public void setSelectedService(String selectedService) { this.selectedService = selectedService; }
        public String getSelectedLocation() { return selectedLocation; }
        public void setSelectedLocation(String selectedLocation) { this.selectedLocation = selectedLocation; }
        public String getLocationSuggestion() { return locationSuggestion; }
        public void setLocationSuggestion(String locationSuggestion) { this.locationSuggestion = locationSuggestion; }
        public Long getSuggestedTenantId() { return suggestedTenantId; }
        public void setSuggestedTenantId(Long suggestedTenantId) { this.suggestedTenantId = suggestedTenantId; }
        public Long getSelectedTenantId() { return selectedTenantId; }
        public void setSelectedTenantId(Long selectedTenantId) { this.selectedTenantId = selectedTenantId; }
        public Long getSelectedBarberId() { return selectedBarberId; }
        public void setSelectedBarberId(Long selectedBarberId) { this.selectedBarberId = selectedBarberId; }
        public List<TenantDto> getAvailableBarbers() { return availableBarbers; }
        public void setAvailableBarbers(List<TenantDto> availableBarbers) { this.availableBarbers = availableBarbers; }
                 public String getBarberList() { return barberList; }
         public void setBarberList(String barberList) { this.barberList = barberList; }
         
         // Session key oluştur
         public String getSessionKey() {
             return phoneNumber + "_" + tenantId;
         }
    }
}
