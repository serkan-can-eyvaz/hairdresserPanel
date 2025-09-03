import React, { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { ArrowLeft, Save, Users } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const CreateTenant = () => {
  const navigate = useNavigate();
  const { token } = useAuth();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    phoneNumber: '',
    email: '',
    address: '',
    timezone: 'Europe/Istanbul',
    city: '',
    district: '',
    neighborhood: '',
    addressDetail: '',
    workingHoursStart: '08:00',
    workingHoursEnd: '22:00',
    breakMinutes: 10
  });
  const [cities, setCities] = useState([]);
  const [districts, setDistricts] = useState([]);
  const [neighborhoods, setNeighborhoods] = useState([]);
  const [availableServices, setAvailableServices] = useState([]);
  const [selectedServices, setSelectedServices] = useState([]);

  const apiBase = 'http://localhost:8080/api';

  useEffect(() => {
    // Şehirleri ve hizmetleri yükle
    const load = async () => {
      try {
        // Şehirleri yükle
        const citiesResponse = await fetch(`${apiBase}/locations/cities`);
        if (citiesResponse.ok) {
          const citiesData = await citiesResponse.json();
          setCities(Array.isArray(citiesData) ? citiesData : []);
        }

        // Hizmetleri yükle (tenant ID 1'den - genel hizmetler)
        const servicesResponse = await fetch(`${apiBase}/services/tenant/1`, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });
        if (servicesResponse.ok) {
          const servicesData = await servicesResponse.json();
          setAvailableServices(Array.isArray(servicesData) ? servicesData : []);
        }
      } catch (e) {
        console.error('Veri yüklenirken hata:', e);
        setCities([]);
        setAvailableServices([]);
      }
    };
    load();
  }, [token]);

  useEffect(() => {
    if (!formData.city) { setDistricts([]); setNeighborhoods([]); return; }
    const loadDistricts = async () => {
      try {
        const r = await fetch(`${apiBase}/locations/cities/${encodeURIComponent(formData.city)}/districts`);
        if (!r.ok) { setDistricts([]); return; }
        const data = await r.json();
        setDistricts(Array.isArray(data) ? data : []);
        setNeighborhoods([]);
        setFormData(prev => ({...prev, district: '', neighborhood: ''}));
      } catch (e) {
        setDistricts([]);
      }
    };
    loadDistricts();
  }, [formData.city]);

  useEffect(() => {
    if (!formData.city || !formData.district) { setNeighborhoods([]); return; }
    const loadNeighborhoods = async () => {
      try {
        const r = await fetch(`${apiBase}/locations/cities/${encodeURIComponent(formData.city)}/districts/${encodeURIComponent(formData.district)}/neighborhoods`);
        if (!r.ok) { setNeighborhoods([]); return; }
        const data = await r.json();
        setNeighborhoods(Array.isArray(data) ? data : []);
      } catch (e) {
        setNeighborhoods([]);
      }
    };
    loadNeighborhoods();
  }, [formData.city, formData.district]);
  const [errors, setErrors] = useState({});

  const timezones = [
    'Europe/Istanbul',
    'Europe/London',
    'Europe/Paris',
    'Europe/Berlin',
    'America/New_York',
    'America/Los_Angeles',
    'Asia/Tokyo',
    'Asia/Shanghai'
  ];

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
    
    // Clear working hours error when either time changes
    if (name === 'workingHoursStart' || name === 'workingHoursEnd') {
      if (errors.workingHours) {
        setErrors(prev => ({
          ...prev,
          workingHours: ''
        }));
      }
    }
  };

  const handleServiceToggle = (service) => {
    setSelectedServices(prev => {
      const isSelected = prev.find(s => s.id === service.id);
      if (isSelected) {
        return prev.filter(s => s.id !== service.id);
      } else {
        return [...prev, {
          ...service,
          price: '',
          currency: 'TRY'
        }];
      }
    });
  };

  const handleServicePriceChange = (serviceId, field, value) => {
    setSelectedServices(prev => 
      prev.map(service => 
        service.id === serviceId 
          ? { ...service, [field]: value }
          : service
      )
    );
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.name.trim()) {
      newErrors.name = 'Kuaför adı gereklidir';
    }

    if (!formData.phoneNumber.trim()) {
      newErrors.phoneNumber = 'Telefon numarası gereklidir';
    } else if (!formData.phoneNumber.startsWith('+')) {
      newErrors.phoneNumber = 'Telefon numarası + ile başlamalıdır (örn: +905321234567)';
    }

    if (formData.email && !/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = 'Geçerli bir email adresi giriniz';
    }

    // Çalışma saatleri kontrolü
    if (formData.workingHoursStart && formData.workingHoursEnd) {
      if (formData.workingHoursStart >= formData.workingHoursEnd) {
        newErrors.workingHours = 'Başlangıç saati bitiş saatinden önce olmalıdır';
      }
    }

    if (formData.breakMinutes < 0 || formData.breakMinutes > 60) {
      newErrors.breakMinutes = 'Mola süresi 0-60 dakika arasında olmalıdır';
    }

    if (selectedServices.length === 0) {
      newErrors.services = 'En az bir hizmet seçmelisiniz';
    }

    // Seçilen hizmetlerin fiyatlarını kontrol et
    for (const service of selectedServices) {
      if (!service.price || service.price <= 0) {
        newErrors.services = 'Tüm seçilen hizmetler için geçerli fiyat girmelisiniz';
        break;
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setLoading(true);

    try {
      const requestData = {
        ...formData,
        services: selectedServices.map(service => ({
          serviceId: service.id,
          price: parseFloat(service.price),
          currency: service.currency
        }))
      };

      const response = await fetch('http://localhost:8080/api/tenants', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestData)
      });

      if (response.ok) {
        const result = await response.json();
        alert('Kuaför başarıyla eklendi!');
        navigate('/tenants');
      } else {
        const errorData = await response.json();
        if (errorData.message) {
          alert(`Hata: ${errorData.message}`);
        } else {
          alert('Kuaför eklenirken bir hata oluştu');
        }
      }
    } catch (error) {
      console.error('Error creating tenant:', error);
      alert('Bağlantı hatası oluştu');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Link
            to="/tenants"
            className="inline-flex items-center px-3 py-2 text-sm font-medium text-gray-600 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            <ArrowLeft className="w-4 h-4 mr-2" />
            Geri Dön
          </Link>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Yeni Kuaför Ekle</h1>
            <p className="text-gray-600">Sisteme yeni bir kuaför ekleyin</p>
          </div>
        </div>
      </div>

      {/* Form */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Basic Information */}
          <div>
            <h3 className="text-lg font-medium text-gray-900 mb-4">Temel Bilgiler</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-2">
                  Kuaför Adı *
                </label>
                <input
                  type="text"
                  id="name"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                    errors.name ? 'border-red-300' : 'border-gray-300'
                  }`}
                  placeholder="Örn: Tahir Kuaför Salonu"
                />
                {errors.name && (
                  <p className="mt-1 text-sm text-red-600">{errors.name}</p>
                )}
              </div>

              <div>
                <label htmlFor="phoneNumber" className="block text-sm font-medium text-gray-700 mb-2">
                  Telefon Numarası *
                </label>
                <input
                  type="tel"
                  id="phoneNumber"
                  name="phoneNumber"
                  value={formData.phoneNumber}
                  onChange={handleInputChange}
                  className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                    errors.phoneNumber ? 'border-red-300' : 'border-gray-300'
                  }`}
                  placeholder="+905321234567"
                />
                {errors.phoneNumber && (
                  <p className="mt-1 text-sm text-red-600">{errors.phoneNumber}</p>
                )}
                <p className="mt-1 text-xs text-gray-500">
                  WhatsApp entegrasyonu için + ile başlamalıdır
                </p>
              </div>
            </div>
          </div>

          {/* Contact Information */}
          <div>
            <h3 className="text-lg font-medium text-gray-900 mb-4">İletişim Bilgileri</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
                  Email Adresi
                </label>
                <input
                  type="email"
                  id="email"
                  name="email"
                  value={formData.email}
                  onChange={handleInputChange}
                  className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                    errors.email ? 'border-red-300' : 'border-gray-300'
                  }`}
                  placeholder="ornek@email.com"
                />
                {errors.email && (
                  <p className="mt-1 text-sm text-red-600">{errors.email}</p>
                )}
              </div>

              <div>
                <label htmlFor="timezone" className="block text-sm font-medium text-gray-700 mb-2">
                  Saat Dilimi
                </label>
                <select
                  id="timezone"
                  name="timezone"
                  value={formData.timezone}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  {timezones.map(tz => (
                    <option key={tz} value={tz}>
                      {tz.replace('_', ' ')}
                    </option>
                  ))}
                </select>
                <p className="mt-1 text-xs text-gray-500">
                  Randevu saatleri bu saat dilimine göre ayarlanacak
                </p>
              </div>
            </div>
          </div>

          {/* Working Hours */}
          <div>
            <h3 className="text-lg font-medium text-gray-900 mb-4">Çalışma Saatleri</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div>
                <label htmlFor="workingHoursStart" className="block text-sm font-medium text-gray-700 mb-2">
                  Başlangıç Saati
                </label>
                <input
                  type="time"
                  id="workingHoursStart"
                  name="workingHoursStart"
                  value={formData.workingHoursStart}
                  onChange={handleInputChange}
                  className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                    errors.workingHours ? 'border-red-300' : 'border-gray-300'
                  }`}
                />
                <p className="mt-1 text-xs text-gray-500">
                  Günlük çalışma başlangıç saati
                </p>
              </div>

              <div>
                <label htmlFor="workingHoursEnd" className="block text-sm font-medium text-gray-700 mb-2">
                  Bitiş Saati
                </label>
                <input
                  type="time"
                  id="workingHoursEnd"
                  name="workingHoursEnd"
                  value={formData.workingHoursEnd}
                  onChange={handleInputChange}
                  className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                    errors.workingHours ? 'border-red-300' : 'border-gray-300'
                  }`}
                />
                <p className="mt-1 text-xs text-gray-500">
                  Günlük çalışma bitiş saati
                </p>
              </div>

              <div>
                <label htmlFor="breakMinutes" className="block text-sm font-medium text-gray-700 mb-2">
                  Mola Süresi (Dakika)
                </label>
                <input
                  type="number"
                  id="breakMinutes"
                  name="breakMinutes"
                  min="0"
                  max="60"
                  value={formData.breakMinutes}
                  onChange={handleInputChange}
                  className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                    errors.breakMinutes ? 'border-red-300' : 'border-gray-300'
                  }`}
                />
                <p className="mt-1 text-xs text-gray-500">
                  Randevular arası mola süresi
                </p>
                {errors.breakMinutes && (
                  <p className="mt-1 text-sm text-red-600">{errors.breakMinutes}</p>
                )}
              </div>
            </div>
            
            {errors.workingHours && (
              <p className="mt-2 text-sm text-red-600">{errors.workingHours}</p>
            )}
            
            <div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
              <div className="flex items-start">
                <div className="flex-shrink-0">
                  <svg className="w-5 h-5 text-blue-600 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
                  </svg>
                </div>
                <div className="ml-3">
                  <h4 className="text-sm font-medium text-blue-900">Akıllı Randevu Sistemi</h4>
                  <div className="mt-2 text-sm text-blue-700 space-y-1">
                    <p>• Sistem otomatik olarak çalışma saatleri içinde uygun randevu saatlerini hesaplar</p>
                    <p>• Her randevu sonrası belirtilen mola süresi kadar bekler</p>
                    <p>• Hizmet süresi + mola süresi toplamı kadar zaman bloklanır</p>
                    <p>• Örnek: 40 dk hizmet + 10 dk mola = 50 dk bloklanır</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Address */}
          <div>
            <h3 className="text-lg font-medium text-gray-900 mb-4">Adres Bilgileri</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">İl</label>
                <select
                  name="city"
                  value={formData.city}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">Seçin</option>
                  {(Array.isArray(cities) ? cities : []).map(c => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">İlçe</label>
                <select
                  name="district"
                  value={formData.district}
                  onChange={handleInputChange}
                  disabled={!formData.city}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100"
                >
                  <option value="">Seçin</option>
                  {districts.map(d => (
                    <option key={d} value={d}>{d}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Mahalle</label>
                <select
                  name="neighborhood"
                  value={formData.neighborhood}
                  onChange={handleInputChange}
                  disabled={!formData.city || !formData.district}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100"
                >
                  <option value="">Seçin</option>
                  {neighborhoods.map(n => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Adres Detayı</label>
                <input
                  type="text"
                  name="addressDetail"
                  value={formData.addressDetail}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Cadde/Sokak/No vb."
                />
              </div>
            </div>
          </div>

          {/* Services Selection */}
          <div>
            <h3 className="text-lg font-medium text-gray-900 mb-4">Hizmet Seçimi</h3>
            <div className="space-y-4">
              <p className="text-sm text-gray-600">
                Bu kuaförün sunacağı hizmetleri seçin ve her biri için fiyat belirleyin:
              </p>
              
              {errors.services && (
                <p className="text-sm text-red-600">{errors.services}</p>
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {availableServices.map((service) => {
                  const isSelected = selectedServices.find(s => s.id === service.id);
                  return (
                    <div key={service.id} className="border border-gray-200 rounded-lg p-4">
                      <div className="flex items-center justify-between mb-3">
                        <div className="flex items-center">
                          <input
                            type="checkbox"
                            id={`service-${service.id}`}
                            checked={!!isSelected}
                            onChange={() => handleServiceToggle(service)}
                            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                          />
                          <label htmlFor={`service-${service.id}`} className="ml-2 text-sm font-medium text-gray-900">
                            {service.name}
                          </label>
                        </div>
                        <span className="text-sm text-gray-500">
                          {service.durationMinutes} dakika
                        </span>
                      </div>
                      
                      {service.description && (
                        <p className="text-xs text-gray-600 mb-3">{service.description}</p>
                      )}
                      
                      {isSelected && (
                        <div className="flex items-center space-x-2">
                          <input
                            type="number"
                            min="0"
                            step="0.01"
                            placeholder="Fiyat"
                            value={isSelected.price}
                            onChange={(e) => handleServicePriceChange(service.id, 'price', e.target.value)}
                            className="flex-1 px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                          />
                          <select
                            value={isSelected.currency}
                            onChange={(e) => handleServicePriceChange(service.id, 'currency', e.target.value)}
                            className="px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                          >
                            <option value="TRY">TRY</option>
                            <option value="USD">USD</option>
                            <option value="EUR">EUR</option>
                          </select>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
              
              {availableServices.length === 0 && (
                <p className="text-sm text-gray-500 text-center py-4">
                  Henüz hizmet tanımlanmamış. Önce <a href="/services" className="text-blue-600 hover:underline">Hizmetler</a> sayfasından hizmet ekleyin.
                </p>
              )}
            </div>
          </div>

          {/* Important Notes */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-start">
              <div className="flex-shrink-0">
                <Users className="w-5 h-5 text-blue-600 mt-0.5" />
              </div>
              <div className="ml-3">
                <h4 className="text-sm font-medium text-blue-900">Önemli Notlar</h4>
                <div className="mt-2 text-sm text-blue-700 space-y-1">
                  <p>• Telefon numarası WhatsApp entegrasyonu için + ile başlamalıdır</p>
                  <p>• Kuaför eklendikten sonra WhatsApp bot otomatik olarak aktif olacaktır</p>
                  <p>• Müşteriler bu numaraya mesaj atarak randevu alabilecektir</p>
                  <p>• Saat dilimi randevu saatlerinin doğru görüntülenmesi için önemlidir</p>
                </div>
              </div>
            </div>
          </div>

          {/* Form Actions */}
          <div className="flex items-center justify-end space-x-4 pt-6 border-t border-gray-200">
            <Link
              to="/tenants"
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            >
              İptal
            </Link>
            <button
              type="submit"
              disabled={loading}
              className="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {loading ? (
                <>
                  <div className="spinner w-4 h-4 mr-2"></div>
                  Ekleniyor...
                </>
              ) : (
                <>
                  <Save className="w-4 h-4 mr-2" />
                  Kuaför Ekle
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateTenant;
