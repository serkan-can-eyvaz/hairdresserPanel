import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, Calendar, Clock, User, Phone, Mail } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const BookAppointment = () => {
  const navigate = useNavigate();
  const { token } = useAuth();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    tenantId: '',
    serviceId: '',
    customerName: '',
    customerPhone: '',
    customerEmail: '',
    appointmentDate: '',
    appointmentTime: '',
    notes: ''
  });
  
  const [tenants, setTenants] = useState([]);
  const [services, setServices] = useState([]);
  const [availableSlots, setAvailableSlots] = useState([]);
  const [errors, setErrors] = useState({});

  const apiBase = 'http://localhost:8080/api';

  useEffect(() => {
    fetchTenants();
  }, []);

  useEffect(() => {
    if (formData.tenantId) {
      fetchServices(formData.tenantId);
    } else {
      setServices([]);
    }
  }, [formData.tenantId]);

  useEffect(() => {
    if (formData.tenantId && formData.appointmentDate) {
      fetchAvailableSlots(formData.tenantId, formData.appointmentDate);
    } else {
      setAvailableSlots([]);
    }
  }, [formData.tenantId, formData.appointmentDate]);

  const fetchTenants = async () => {
    try {
      const response = await fetch(`${apiBase}/admin/tenants`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        // Sadece aktif kuaförleri filtrele ve "Sistem Yönetimi"ni hariç tut
        const activeTenants = (data.content || data || []).filter(tenant => 
          tenant.active && tenant.name !== 'Sistem Yönetimi'
        );
        setTenants(activeTenants);
      }
    } catch (error) {
      console.error('Kuaförler yüklenirken hata:', error);
    }
  };

  const fetchServices = async (tenantId) => {
    try {
      const response = await fetch(`${apiBase}/services/tenant/${tenantId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setServices(Array.isArray(data) ? data : []);
      }
    } catch (error) {
      console.error('Hizmetler yüklenirken hata:', error);
    }
  };

  const fetchAvailableSlots = async (tenantId, date) => {
    try {
      const response = await fetch(`${apiBase}/slots/available?tenantId=${tenantId}&date=${date}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });
      
      if (response.ok) {
        const data = await response.json();
        setAvailableSlots(Array.isArray(data) ? data : []);
      }
    } catch (error) {
      console.error('Müsait saatler yüklenirken hata:', error);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Hata temizle
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  const validateForm = () => {
    const newErrors = {};
    
    if (!formData.tenantId) newErrors.tenantId = 'Kuaför seçimi zorunludur';
    if (!formData.serviceId) newErrors.serviceId = 'Hizmet seçimi zorunludur';
    if (!formData.customerName.trim()) newErrors.customerName = 'Müşteri adı zorunludur';
    if (!formData.customerPhone.trim()) newErrors.customerPhone = 'Telefon numarası zorunludur';
    if (!formData.appointmentDate) newErrors.appointmentDate = 'Tarih seçimi zorunludur';
    if (!formData.appointmentTime) newErrors.appointmentTime = 'Saat seçimi zorunludur';
    
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
      // Önce müşteriyi bul veya oluştur
      let customerId;
      try {
        const customerResponse = await fetch(`${apiBase}/customers`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            name: formData.customerName,
            phoneNumber: formData.customerPhone,
            email: formData.customerEmail || null,
            tenantId: parseInt(formData.tenantId)
          })
        });
        
        if (customerResponse.ok) {
          const customerData = await customerResponse.json();
          customerId = customerData.id;
        } else {
          throw new Error('Müşteri oluşturulamadı');
        }
      } catch (error) {
        console.error('Müşteri oluşturma hatası:', error);
        throw error;
      }

      // Randevuyu oluştur
      const appointmentData = {
        tenantId: parseInt(formData.tenantId),
        customerId: customerId,
        serviceId: parseInt(formData.serviceId),
        startTime: `${formData.appointmentDate}T${formData.appointmentTime}:00`,
        notes: formData.notes || null
      };

      const appointmentResponse = await fetch(`${apiBase}/appointments`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(appointmentData)
      });

      if (appointmentResponse.ok) {
        alert('Randevu başarıyla oluşturuldu!');
        navigate('/appointments');
      } else {
        const errorData = await appointmentResponse.json();
        throw new Error(errorData.message || 'Randevu oluşturulamadı');
      }
    } catch (error) {
      console.error('Randevu oluşturma hatası:', error);
      alert('Randevu oluşturulurken hata oluştu: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount, currency = 'TRY') => {
    return new Intl.NumberFormat('tr-TR', {
      style: 'currency',
      currency
    }).format(amount);
  };

  const getSelectedService = () => {
    return services.find(service => service.id === parseInt(formData.serviceId));
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto py-8 px-4">
        {/* Header */}
        <div className="mb-8">
          <button
            onClick={() => navigate(-1)}
            className="flex items-center text-gray-600 hover:text-gray-800 mb-4"
          >
            <ArrowLeft className="w-5 h-5 mr-2" />
            Geri Dön
          </button>
          
          <div className="flex items-center mb-2">
            <Calendar className="w-8 h-8 text-blue-600 mr-3" />
            <h1 className="text-3xl font-bold text-gray-900">Yeni Randevu</h1>
          </div>
          <p className="text-gray-600">Müşteri için yeni randevu oluşturun</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-8">
          {/* Kuaför ve Hizmet Seçimi */}
          <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-6">Kuaför ve Hizmet Seçimi</h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Kuaför *
                </label>
                <select
                  name="tenantId"
                  value={formData.tenantId}
                  onChange={handleInputChange}
                  className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                    errors.tenantId ? 'border-red-300' : 'border-gray-300'
                  }`}
                >
                  <option value="">Kuaför seçin</option>
                  {tenants.map(tenant => (
                    <option key={tenant.id} value={tenant.id}>
                      {tenant.name}
                    </option>
                  ))}
                </select>
                {errors.tenantId && (
                  <p className="mt-1 text-sm text-red-600">{errors.tenantId}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Hizmet *
                </label>
                <select
                  name="serviceId"
                  value={formData.serviceId}
                  onChange={handleInputChange}
                  disabled={!formData.tenantId}
                  className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100 ${
                    errors.serviceId ? 'border-red-300' : 'border-gray-300'
                  }`}
                >
                  <option value="">Hizmet seçin</option>
                  {services.map(service => (
                    <option key={service.id} value={service.id}>
                      {service.name} - {formatCurrency(service.price)} ({service.durationMinutes} dk)
                    </option>
                  ))}
                </select>
                {errors.serviceId && (
                  <p className="mt-1 text-sm text-red-600">{errors.serviceId}</p>
                )}
              </div>
            </div>

            {/* Seçilen hizmet bilgileri */}
            {getSelectedService() && (
              <div className="mt-4 p-4 bg-blue-50 rounded-lg">
                <h3 className="font-medium text-blue-900 mb-2">Seçilen Hizmet</h3>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-blue-700">Hizmet:</span>
                    <span className="ml-2 text-blue-900">{getSelectedService().name}</span>
                  </div>
                  <div>
                    <span className="text-blue-700">Süre:</span>
                    <span className="ml-2 text-blue-900">{getSelectedService().durationMinutes} dakika</span>
                  </div>
                  <div>
                    <span className="text-blue-700">Fiyat:</span>
                    <span className="ml-2 text-blue-900">{formatCurrency(getSelectedService().price)}</span>
                  </div>
                  <div>
                    <span className="text-blue-700">Açıklama:</span>
                    <span className="ml-2 text-blue-900">{getSelectedService().description}</span>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Müşteri Bilgileri */}
          <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-6 flex items-center">
              <User className="w-5 h-5 mr-2" />
              Müşteri Bilgileri
            </h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Ad Soyad *
                </label>
                <input
                  type="text"
                  name="customerName"
                  value={formData.customerName}
                  onChange={handleInputChange}
                  className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                    errors.customerName ? 'border-red-300' : 'border-gray-300'
                  }`}
                  placeholder="Müşteri adı"
                />
                {errors.customerName && (
                  <p className="mt-1 text-sm text-red-600">{errors.customerName}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2 flex items-center">
                  <Phone className="w-4 h-4 mr-1" />
                  Telefon *
                </label>
                <input
                  type="tel"
                  name="customerPhone"
                  value={formData.customerPhone}
                  onChange={handleInputChange}
                  className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                    errors.customerPhone ? 'border-red-300' : 'border-gray-300'
                  }`}
                  placeholder="+90 5XX XXX XX XX"
                />
                {errors.customerPhone && (
                  <p className="mt-1 text-sm text-red-600">{errors.customerPhone}</p>
                )}
              </div>

              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-2 flex items-center">
                  <Mail className="w-4 h-4 mr-1" />
                  Email (Opsiyonel)
                </label>
                <input
                  type="email"
                  name="customerEmail"
                  value={formData.customerEmail}
                  onChange={handleInputChange}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="ornek@email.com"
                />
              </div>
            </div>
          </div>

          {/* Tarih ve Saat Seçimi */}
          <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-6 flex items-center">
              <Clock className="w-5 h-5 mr-2" />
              Tarih ve Saat Seçimi
            </h2>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Tarih *
                </label>
                <input
                  type="date"
                  name="appointmentDate"
                  value={formData.appointmentDate}
                  onChange={handleInputChange}
                  min={new Date().toISOString().split('T')[0]}
                  className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent ${
                    errors.appointmentDate ? 'border-red-300' : 'border-gray-300'
                  }`}
                />
                {errors.appointmentDate && (
                  <p className="mt-1 text-sm text-red-600">{errors.appointmentDate}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Saat *
                </label>
                <select
                  name="appointmentTime"
                  value={formData.appointmentTime}
                  onChange={handleInputChange}
                  disabled={!formData.appointmentDate || availableSlots.length === 0}
                  className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100 ${
                    errors.appointmentTime ? 'border-red-300' : 'border-gray-300'
                  }`}
                >
                  <option value="">Saat seçin</option>
                  {availableSlots.map(slot => (
                    <option key={slot.startTime} value={slot.startTime}>
                      {slot.startTime} - {slot.endTime}
                    </option>
                  ))}
                </select>
                {errors.appointmentTime && (
                  <p className="mt-1 text-sm text-red-600">{errors.appointmentTime}</p>
                )}
                {formData.appointmentDate && availableSlots.length === 0 && (
                  <p className="mt-1 text-sm text-yellow-600">
                    Bu tarih için müsait saat bulunmuyor
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* Notlar */}
          <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-6">Ek Bilgiler</h2>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Notlar
              </label>
              <textarea
                name="notes"
                value={formData.notes}
                onChange={handleInputChange}
                rows={4}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Randevu ile ilgili özel notlar..."
              />
            </div>
          </div>

          {/* Submit Button */}
          <div className="flex justify-end space-x-4">
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              İptal
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Oluşturuluyor...' : 'Randevu Oluştur'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default BookAppointment;
