# Kuaför Otomasyon Sistemi

Bu proje, kuaförler için WhatsApp tabanlı randevu otomasyon sistemi sağlar. AI destekli chatbot ile müşteriler kolayca randevu alabilir ve kuaförler randevularını yönetebilir.

## 🚀 Özellikler

- **WhatsApp Bot**: AI destekli doğal dil işleme ile randevu alma
- **Admin Panel**: Modern ve kullanıcı dostu yönetim arayüzü
- **Akıllı Randevu Sistemi**: Çalışma saatleri, hizmet süresi ve mola süresi dikkate alınarak otomatik randevu planlama
- **Çoklu Kuaför Desteği**: Her kuaför kendi WhatsApp numarası ile çalışabilir
- **Gerçek Zamanlı Bildirimler**: Randevu onayları ve hatırlatmalar
- **Kapsamlı Raporlama**: Performans analizi ve istatistikler

## 🛠️ Teknolojiler

### Backend
- **Spring Boot 3.x**: Java backend framework
- **PostgreSQL**: Veritabanı
- **JPA/Hibernate**: ORM
- **Spring Security**: Güvenlik
- **Swagger/OpenAPI**: API dokümantasyonu

### Frontend
- **React.js**: Modern UI framework
- **Tailwind CSS**: Styling
- **Lucide React**: İkonlar
- **Axios**: HTTP client

### AI & WhatsApp
- **OpenAI GPT-4**: Doğal dil işleme
- **WhatsApp Business API**: Mesajlaşma
- **Twilio**: Alternatif WhatsApp entegrasyonu

## 📋 Kurulum

### Gereksinimler
- Java 17+
- Node.js 18+
- PostgreSQL 13+
- Python 3.8+ (AI Agent için)

### 1. Repository'yi klonlayın
```bash
git clone https://github.com/serkan-can-eyvaz/hairdresserPanel.git
cd hairdresserPanel
```

### 2. Environment Variables
`env.example` dosyasını `.env` olarak kopyalayın ve gerekli değerleri doldurun:

```bash
cp env.example .env
```

Gerekli environment variables:
- `OPENAI_API_KEY`: OpenAI API anahtarınız
- `WHATSAPP_API_TOKEN`: WhatsApp Business API token'ınız
- `WHATSAPP_PHONE_NUMBER_ID`: WhatsApp telefon numarası ID'niz
- `TWILIO_ACCOUNT_SID`: Twilio hesap SID'iniz (opsiyonel)
- `TWILIO_AUTH_TOKEN`: Twilio auth token'ınız (opsiyonel)

### 3. Backend Kurulumu
```bash
cd back-end/barber-automation
mvn clean install
mvn spring-boot:run
```

Backend `http://localhost:8080` adresinde çalışacak.

### 4. Frontend Kurulumu
```bash
cd frontend
npm install
npm start
```

Frontend `http://localhost:3000` adresinde çalışacak.

### 5. AI Agent Kurulumu (Opsiyonel)
```bash
cd ai-agent
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 4002
```

## 🔧 Konfigürasyon

### WhatsApp Business API
1. Meta Business Manager'da WhatsApp Business hesabı oluşturun
2. API token'ınızı alın
3. Webhook URL'ini ayarlayın: `https://yourdomain.com/api/webhook/whatsapp`

### Twilio Sandbox (Test için)
1. Twilio hesabı oluşturun
2. WhatsApp Sandbox'ı aktifleştirin
3. Sandbox numarasını kullanarak test edin

## 📱 Kullanım

### Müşteri Tarafı
1. WhatsApp'tan kuaförün numarasına mesaj gönderin
2. Bot size şehir/ilçe seçimi sunacak
3. Kuaför seçimi yapın
4. Adınızı girin
5. Hizmet seçin
6. Tarih ve saat belirleyin
7. Randevunuzu onaylayın

### Admin Panel
1. `http://localhost:3000` adresine gidin
2. Admin paneline giriş yapın
3. Randevuları görüntüleyin ve yönetin
4. Kuaför ekleyin ve düzenleyin
5. Raporları inceleyin

## 🏗️ Proje Yapısı

```
hairdresserPanel/
├── back-end/barber-automation/     # Spring Boot backend
├── frontend/                       # React frontend
├── ai-agent/                       # Python AI agent
├── .gitignore                      # Git ignore rules
├── env.example                     # Environment variables template
└── README.md                       # Bu dosya
```

## 🔒 Güvenlik

- Tüm API anahtarları environment variables olarak saklanır
- JWT token tabanlı kimlik doğrulama
- HTTPS zorunlu (production)
- Input validation ve sanitization

## 📊 API Dokümantasyonu

Backend çalıştıktan sonra Swagger UI'ya erişin:
`http://localhost:8080/api/swagger-ui.html`

## 🤝 Katkıda Bulunma

1. Fork yapın
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Commit yapın (`git commit -m 'Add amazing feature'`)
4. Push yapın (`git push origin feature/amazing-feature`)
5. Pull Request oluşturun

## 📄 Lisans

Bu proje MIT lisansı altında lisanslanmıştır.

## 📞 Destek

Herhangi bir sorun yaşarsanız:
- Issue oluşturun
- Email: support@example.com

## 🎯 Gelecek Özellikler

- [ ] Mobil uygulama
- [ ] SMS bildirimleri
- [ ] Çoklu dil desteği
- [ ] Ödeme entegrasyonu
- [ ] Müşteri değerlendirme sistemi