from typing import Optional, Dict, Any
from ..core.config import settings
from .tools import get_tool_service
import re

try:
    from openai import OpenAI  # type: ignore
except Exception:  # pragma: no cover
    OpenAI = None  # type: ignore


class NLUService:
    def __init__(self) -> None:
        print(f"=== AI AGENT INIT DEBUG ===")
        print(f"OpenAI API Key: {settings.openai_api_key[:20] if settings.openai_api_key else 'None'}...")
        print(f"OpenAI Model: {settings.openai_model}")
        print(f"OpenAI available: {OpenAI is not None}")
        
        self.enabled = bool(settings.openai_api_key and OpenAI)
        print(f"AI Agent enabled: {self.enabled}")
        
        self.client = OpenAI(api_key=settings.openai_api_key) if self.enabled else None
        print(f"OpenAI client created: {self.client is not None}")
        print(f"=== AI AGENT INIT COMPLETE ===")
        
        # Session storage for conversation state
        self.sessions = {}

    def _parse_location(self, text: str) -> tuple[str, Optional[str]]:
        """Parse user location string to (city, district). Accepts 'City, District' or 'City District'."""
        raw = text.strip()
        if "," in raw:
            parts = [p.strip() for p in raw.split(",", 1)]
            city = parts[0]
            district = parts[1] if len(parts) > 1 and parts[1] else None
            return city, district
        # Fallback: split by whitespace into two tokens
        tokens = re.split(r"\s+", raw)
        if len(tokens) >= 2:
            city = tokens[0]
            district = " ".join(tokens[1:])
            return city, district
        return raw, None

    def process_message(self, message: str, tenant_id: int, from_number: str) -> Dict[str, Any]:
        """Process message with advanced AI understanding and context awareness."""
        if not message:
            return {"ok": False, "intent": "unknown", "reply": "Mesajınızı anlayamadım."}

        # Get or create session
        session_key = f"{tenant_id}_{from_number}"
        if session_key not in self.sessions:
            self.sessions[session_key] = {
                "state": "awaiting_location",
                "customer_name": None,
                "location": None,
                "available_barbers": [],
                "selected_barber": None,
                "selected_services": [],
                "date": None,
                "time": None
            }
        
        session = self.sessions[session_key]
        current_state = session["state"]

        if not self.enabled:
            return self._rule_based_fallback(message, session)

        try:
            system_prompt = self._create_system_prompt()
            user_prompt = self._create_user_prompt(message, tenant_id, from_number, session)
            
            response = self.client.chat.completions.create(
                model=settings.openai_model,
                temperature=0.7,
                max_tokens=500,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt},
                ],
                tools=[{ "type": "function", "function": { "name": "process_appointment_request", "description": "Process appointment request with context awareness", "parameters": { "type": "object", "properties": { "intent": { "type": "string", "enum": ["greeting", "appointment_start", "provide_location", "select_barber", "provide_name", "provide_service", "provide_date", "provide_time", "confirm_appointment", "cancel_appointment", "unknown"] }, "reply": { "type": "string" }, "next_state": { "type": "string", "enum": ["awaiting_location", "awaiting_barber_selection", "awaiting_name", "awaiting_service", "awaiting_date", "awaiting_time", "awaiting_confirmation", "completed"] }, "extracted_info": { "type": "object", "properties": { "customer_name": {"type": "string"}, "location_preference": {"type": "string"}, "barber_selection": {"type": "string"}, "service_preference": {"type": "string"}, "date_preference": {"type": "string"}, "time_preference": {"type": "string"} } } }, "required": ["intent", "reply"] } } }],
                tool_choice={"type": "function", "function": {"name": "process_appointment_request"}}
            )
            
            # Parse tool call response (fallback to content if tools absent)
            result = {"ok": True, "intent": "unknown", "reply": "", "extracted_info": {}}
            tool_calls = response.choices[0].message.tool_calls
            if tool_calls and len(tool_calls) > 0:
                import json
                result = json.loads(tool_calls[0].function.arguments)
            else:
                # Minimal parse from content
                content = response.choices[0].message.content or ""
                result["reply"] = content

            # Always enforce real DB listing when location is provided or expected
            should_list = result.get("intent") == "provide_location" or current_state == "awaiting_location"
            loc = result.get("extracted_info", {}).get("location_preference") if isinstance(result.get("extracted_info"), dict) else None
            if not loc and ("," in message or " " in message) and should_list:
                city, district = self._parse_location(message)
            else:
                city, district = self._parse_location(loc) if loc else (None, None)

            if city:
                tools = get_tool_service()
                data = tools.list_tenants_by_location_sync(city, district)
                if data.get("success"):
                    tenants = data.get("tenants", [])
                    extracted = result.get("extracted_info", {}) or {}
                    # Put options even if empty; agent can phrase accordingly
                    options = []
                    for t in tenants:
                        options.append({"id": t.get("id"), "name": t.get("name"), "address": t.get("neighborhood") or t.get("address") or ""})
                    extracted["barber_options"] = options
                    extracted["location_preference"] = f"{city}{(', ' + district) if district else ''}"
                    result["extracted_info"] = extracted
                    result["next_state"] = "awaiting_barber_selection" if options else "awaiting_location"

                    # Let GPT phrase the reply using real options
                    options_text = "\n".join([f"{i+1}. {o['name']} - {o['address']}" for i, o in enumerate(options)]) or "(Bu bölgede aktif kuaför bulunamadı)"
                    phrasing_prompt = (
                        "Aşağıdaki gerçek kuaför listesini kullanarak Türkçe, samimi ve kısa bir yanıt yaz. "
                        "Eğer liste boş ise nazikçe bu bölgede aktif kuaför bulunmadığını söyle ve farklı bölge iste. "
                        "Liste dolu ise kullanıcıya numara ile seçim yapmasını söyle.\n\n"
                        f"Bölge: {city}{(', ' + district) if district else ''}\n"
                        f"Kuaförler:\n{options_text}"
                    )
                    phrasing = self.client.chat.completions.create(
                        model=settings.openai_model,
                        temperature=0.6,
                        max_tokens=250,
                        messages=[
                            {"role": "system", "content": "Profesyonel kuaför randevu asistanısın."},
                            {"role": "user", "content": phrasing_prompt}
                        ]
                    )
                    result["reply"] = phrasing.choices[0].message.content or result.get("reply", "")

            # Update session
            self._update_session(session, result, message)
            
            return {
                "ok": True,
                "intent": result.get("intent", "unknown"),
                "reply": result.get("reply", "Mesajınızı aldım. Nasıl yardımcı olabilirim?"),
                "next_state": result.get("next_state"),
                "extracted_info": result.get("extracted_info", {})
            }
            
        except Exception as e:
            print(f"OpenAI API error: {e}")
            return self._rule_based_fallback(message, session)

    def _update_session(self, session: Dict, result: Dict, message: str):
        """Update session with new information."""
        intent = result.get("intent")
        extracted_info = result.get("extracted_info", {})
        
        if intent == "provide_location":
            session["location"] = extracted_info.get("location_preference", message.strip())
            session["state"] = "awaiting_barber_selection"
        elif intent == "select_barber":
            session["selected_barber"] = extracted_info.get("barber_selection", message.strip())
            session["state"] = "awaiting_name"
        elif intent == "provide_name":
            session["customer_name"] = extracted_info.get("customer_name", message.strip())
            session["state"] = "awaiting_service"
        elif intent == "provide_service":
            service = extracted_info.get("service_preference", message.strip())
            if service not in session["selected_services"]:
                session["selected_services"].append(service)
            session["state"] = "awaiting_date"
        elif intent == "provide_date":
            session["date"] = extracted_info.get("date_preference", message.strip())
            session["state"] = "awaiting_time"
        elif intent == "provide_time":
            session["time"] = extracted_info.get("time_preference", message.strip())
            session["state"] = "awaiting_confirmation"
        elif intent == "confirm_appointment":
            session["state"] = "completed"
        elif intent == "greeting" or intent == "appointment_start":
            session["state"] = "awaiting_location"

    def _create_system_prompt(self) -> str:
        return """Sen profesyonel bir kuaför salonu randevu botusun. Müşteri ile doğal ve samimi bir şekilde konuşuyorsun.

GÖREV:
- Müşteri mesajlarını analiz et
- Uygun niyet (intent) belirle
- Doğal, samimi ve yardımcı yanıtlar ver
- Türkçe konuş, emoji kullan
- Müşteri bilgilerini (konum, kuaför seçimi, isim, hizmet, tarih, saat) çıkar

ÖNEMLİ KURALLAR:
1. GREETING: "merhaba", "selam", "hello", "hi", "hey" gibi selamlaşma kelimeleri için greeting intent
2. RANDEVU: "randevu", "appointment", "rezervasyon" kelimeleri için appointment_start intent
3. KONUM: Şehir/ilçe bilgisi için provide_location intent (örn: "Ankara Çankaya", "İstanbul Kadıköy")
4. KUAFÖR SEÇİMİ: Kuaför seçimi için select_barber intent (örn: "1", "Baran Kuaför", "ikincisini")
5. İSİM: Müşteri ismi için provide_name intent
6. HİZMET: Hizmet seçimi için provide_service intent
7. TARİH: Tarih bilgisi için provide_date intent
8. SAAT: Saat bilgisi için provide_time intent

KONUŞMA TARZI:
- Samimi ve profesyonel
- Emoji kullan (😊, ✅, 📅, ⏰, 💰, 👋, 🏠, ✂️, 💇‍♂️)
- Kısa ve net
- Yardımcı ve anlayışlı

YENİ RANDEVU AKIŞI:
1. Karşılama (greeting) → awaiting_location
2. Konum alma (provide_location) → awaiting_barber_selection
3. Kuaför seçimi (select_barber) → awaiting_name
4. İsim alma (provide_name) → awaiting_service
5. Hizmet seçimi (provide_service) → awaiting_date
6. Tarih seçimi (provide_date) → awaiting_time
7. Saat seçimi (provide_time) → awaiting_confirmation
8. Onay (confirm_appointment) → completed

HER DURUMDA:
- Müşteriye yardımcı ol
- Bir sonraki adımı net şekilde belirt
- Eksik bilgileri nazikçe iste
- Pozitif ve motive edici ol
- Konum bazlı kuaför listeleme yap

ÖNEMLİ: Session state'e göre uygun yanıt ver!
ÖNEMLİ: Konum bilgisi alındıktan sonra o bölgedeki kuaförleri listele!"""

    def _create_user_prompt(self, message: str, tenant_id: int, from_number: str, session: Dict) -> str:
        current_state = session["state"]
        customer_name = session.get("customer_name", "Müşteri")
        location = session.get("location", "Henüz belirtilmedi")
        selected_barber = session.get("selected_barber", "Henüz seçilmedi")
        services = session.get("selected_services", [])
        
        return f"""Müşteri mesajı: "{message}"

MEVCUT DURUM:
- State: {current_state}
- Müşteri adı: {customer_name}
- Konum: {location}
- Seçilen kuaför: {selected_barber}
- Seçilen hizmetler: {', '.join(services) if services else 'Henüz seçilmedi'}

ÖNEMLİ KURALLAR:
1. GREETING: "merhaba", "selam", "hello", "hi", "hey" gibi selamlaşma kelimeleri için greeting intent
2. RANDEVU: "randevu", "appointment", "rezervasyon" kelimeleri için appointment_start intent
3. KONUM: Şehir/ilçe bilgisi için provide_location intent (örn: "Ankara Çankaya", "İstanbul Kadıköy")
4. KUAFÖR SEÇİMİ: Kuaför seçimi için select_barber intent (örn: "1", "Baran Kuaför", "ikincisini")
5. İSİM: Müşteri ismi için provide_name intent
6. HİZMET: Hizmet seçimi için provide_service intent
7. TARİH: Tarih bilgisi için provide_date intent
8. SAAT: Saat bilgisi için provide_time intent

ÖRNEKLER:
- "Merhaba" → greeting intent
- "Randevu almak istiyorum" → appointment_start intent  
- "Ankara Çankaya" → provide_location intent
- "İstanbul Kadıköy" → provide_location intent
- "1" → select_barber intent (kuaför seçimi)
- "Baran Kuaför" → select_barber intent (kuaför seçimi)
- "Tahir Tolu" → provide_name intent
- "Ali Can" → provide_name intent
- "Saç, Sakal" → provide_service intent
- "Yarın" → provide_date intent
- "14:00" → provide_time intent

Mesajı analiz et ve uygun yanıtı ver. Session state'e göre bir sonraki adımı belirt!
ÖNEMLİ: Konum bilgisi alındıktan sonra o bölgedeki kuaförleri listele!"""

    def _rule_based_fallback(self, message: str, session: Dict) -> Dict[str, Any]:
        """Fallback to rule-based processing if AI is not available."""
        lower = message.lower().strip()
        current_state = session["state"]
        
        # Greeting detection
        if any(word in lower for word in ["merhaba", "selam", "hello", "hi", "hey"]):
            return {
                "ok": True,
                "intent": "greeting",
                "reply": "Merhaba! 👋 Randevu sistemimize hoş geldiniz. Hangi şehir ve ilçede kuaför arıyorsunuz? 🏠",
                "next_state": "awaiting_location"
            }
        
        # Appointment start detection
        if any(word in lower for word in ["randevu", "appointment", "rezervasyon", "tarih"]):
            return {
                "ok": True,
                "intent": "appointment_start",
                "reply": "Harika! Randevu almak için önce adınızı öğrenebilir miyim? 😊",
                "next_state": "awaiting_name"
            }
        
        # Name detection - Daha spesifik kurallar
        if current_state == "awaiting_name":
            words = message.strip().split()
            if (len(words) <= 3 and 
                not any(keyword in lower for keyword in ["merhaba", "selam", "hello", "hi", "hey", "randevu", "appointment", "rezervasyon"]) and
                not any(word.isdigit() for word in words)):
                return {
                    "ok": True,
                    "intent": "provide_name",
                    "reply": f"Teşekkürler! {message.strip()} olarak kaydettim. 👋 Şimdi hangi hizmeti istiyorsunuz? (1) Saç, (2) Sakal, (3) Saç Yıkama, (4) Fön",
                    "next_state": "awaiting_service"
                }
        
        # Service selection
        if current_state == "awaiting_service":
            if message.strip() in ["1", "2", "3", "4"]:
                services = ["Saç", "Sakal", "Saç Yıkama", "Fön"]
                service_index = int(message.strip()) - 1
                if 0 <= service_index < len(services):
                    service_name = services[service_index]
                    if service_name not in session["services"]:
                        session["services"].append(service_name)
                    return {
                        "ok": True,
                        "intent": "provide_service",
                        "reply": f"✅ {service_name} hizmeti seçildi! Başka hizmet eklemek istiyor musunuz? Yoksa devam etmek için 'tamam' yazın.",
                        "next_state": "awaiting_service"
                    }
            elif "tamam" in lower or "yeterli" in lower or "devam" in lower:
                selected_services = ", ".join(session["services"])
                return {
                    "ok": True,
                    "intent": "provide_service",
                    "reply": f"Harika! Seçilen hizmetler: {selected_services} 🎯 Şimdi hangi şehir ve ilçede hizmet almak istiyorsunuz? (Örn: İstanbul, Kadıköy)",
                    "next_state": "awaiting_location"
                }
            else:
                # Service names in text
                service_keywords = ["saç", "sakal", "yıkama", "fön"]
                found_services = []
                for keyword in service_keywords:
                    if keyword in lower:
                        if keyword == "saç" and "yıkama" in lower:
                            found_services.append("Saç Yıkama")
                        elif keyword == "saç":
                            found_services.append("Saç")
                        elif keyword == "sakal":
                            found_services.append("Sakal")
                        elif keyword == "fön":
                            found_services.append("Fön")
                
                for service in found_services:
                    if service not in session["services"]:
                        session["services"].append(service)
                
                if found_services:
                    return {
                        "ok": True,
                        "intent": "provide_service",
                        "reply": f"✅ {', '.join(found_services)} hizmeti seçildi! Başka hizmet eklemek istiyor musunuz? Yoksa devam etmek için 'tamam' yazın.",
                        "next_state": "awaiting_service"
                    }
        
        # Location detection
        if current_state == "awaiting_location":
            return {
                "ok": True,
                "intent": "provide_location",
                "reply": f"Teşekkürler! {message.strip()} olarak kaydettim. 🏠 Şimdi hangi tarih için randevu istiyorsunuz? Bugün, yarın veya başka bir tarih? 📅",
                "next_state": "awaiting_date"
            }
        
        # Date detection
        if current_state == "awaiting_date":
            if any(word in lower for word in ["bugün", "yarın", "pazartesi", "salı", "çarşamba", "perşembe", "cuma", "cumartesi", "pazar"]):
                return {
                    "ok": True,
                    "intent": "provide_date",
                    "reply": f"Harika! {message.strip()} için randevu alıyoruz. 📅 Şimdi hangi saati istiyorsunuz? Müsait saatler: 09:00, 10:00, 11:00, 14:00, 15:00, 16:00",
                    "next_state": "awaiting_time"
                }
        
        # Time detection
        if current_state == "awaiting_time":
            if any(word in lower for word in ["9", "10", "11", "14", "15", "16", "09", "10:00", "11:00", "14:00", "15:00", "16:00"]):
                return {
                    "ok": True,
                    "intent": "provide_time",
                    "reply": f"Mükemmel! Saat {message.strip()} için randevu alıyoruz. ⏰ Randevu özeti:\n\n📋 Hizmet: {', '.join(session['services'])}\n🏠 Konum: {session.get('location', 'Belirtilmedi')}\n📅 Tarih: {session.get('date', 'Belirtilmedi')}\n⏰ Saat: {message.strip()}\n\nOnaylamak için 'evet' yazın, iptal için 'hayır' yazın.",
                    "next_state": "awaiting_confirmation"
                }
        
        # Confirmation
        if current_state == "awaiting_confirmation":
            if any(word in lower for word in ["evet", "e", "yes", "tamam", "onaylıyorum"]):
                return {
                    "ok": True,
                    "intent": "confirm_appointment",
                    "reply": "🎉 Randevunuz başarıyla oluşturuldu! Zamanında bekleriz. 😊",
                    "next_state": "completed"
                }
            elif any(word in lower for word in ["hayır", "h", "no", "iptal", "vazgeç"]):
                return {
                    "ok": True,
                    "intent": "cancel_appointment",
                    "reply": "Randevu iptal edildi. Yeni bir randevu almak için 'randevu' yazabilirsiniz. 😊",
                    "next_state": "awaiting_name"
                }
        
        # Default response based on state
        if current_state == "awaiting_name":
            return {
                "ok": True,
                "intent": "unknown",
                "reply": "Lütfen adınızı söyleyin. 😊",
                "next_state": "awaiting_name"
            }
        elif current_state == "awaiting_service":
            return {
                "ok": True,
                "intent": "unknown",
                "reply": "Hangi hizmeti istiyorsunuz? (1) Saç, (2) Sakal, (3) Saç Yıkama, (4) Fön",
                "next_state": "awaiting_service"
            }
        elif current_state == "awaiting_location":
            return {
                "ok": True,
                "intent": "unknown",
                "reply": "Hangi şehir ve ilçede hizmet almak istiyorsunuz? (Örn: İstanbul, Kadıköy)",
                "next_state": "awaiting_location"
            }
        elif current_state == "awaiting_date":
            return {
                "ok": True,
                "intent": "unknown",
                "reply": "Hangi tarih için randevu istiyorsunuz? Bugün, yarın veya başka bir tarih?",
                "next_state": "awaiting_date"
            }
        elif current_state == "awaiting_time":
            return {
                "ok": True,
                "intent": "unknown",
                "reply": "Hangi saati istiyorsunuz? Müsait saatler: 09:00, 10:00, 11:00, 14:00, 15:00, 16:00",
                "next_state": "awaiting_time"
            }
        else:
            return {
                "ok": True,
                "intent": "unknown",
                "reply": "Mesajınızı aldım. Randevu almak için 'randevu' yazabilir veya doğrudan adınızı söyleyebilirsiniz. 😊",
                "next_state": "awaiting_name"
            }


nlu_service = NLUService()


