import React, { useState, useEffect } from 'react';
import api from '../utils/api';

const Settings = () => {
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [settings, setSettings] = useState({
    system: {
      appName: 'Kuaför Randevu Sistemi',
      timezone: 'Europe/Istanbul',
      currency: 'TRY',
      language: 'tr'
    },
    whatsapp: {
      enabled: true,
      webhookUrl: 'https://your-domain.com/api/webhook/whatsapp',
      verifyToken: 'barber_automation_token',
      sandboxMode: true
    },
    notifications: {
      emailEnabled: true,
      smsEnabled: false,
      whatsappEnabled: true,
      reminderHours: 24
    },
    business: {
      workingHours: {
        start: '09:00',
        end: '18:00'
      },
      workingDays: ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday'],
      appointmentDuration: 30,
      maxAdvanceDays: 30
    }
  });

  const [activeTab, setActiveTab] = useState('system');

  const handleInputChange = (section, field, value) => {
    setSettings(prev => ({
      ...prev,
      [section]: {
        ...prev[section],
        [field]: value
      }
    }));
  };

  const handleNestedInputChange = (section, subsection, field, value) => {
    setSettings(prev => ({
      ...prev,
      [section]: {
        ...prev[section],
        [subsection]: {
          ...prev[section][subsection],
          [field]: value
        }
      }
    }));
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      // Gerçek uygulamada backend'e gönderilecek
      await new Promise(resolve => setTimeout(resolve, 1000));
      alert('Ayarlar başarıyla kaydedildi!');
    } catch (error) {
      console.error('Ayarlar kaydedilirken hata:', error);
      alert('Ayarlar kaydedilirken bir hata oluştu!');
    } finally {
      setSaving(false);
    }
  };

  const tabs = [
    { id: 'system', name: 'Sistem', icon: '⚙️' },
    { id: 'whatsapp', name: 'WhatsApp', icon: '💬' },
    { id: 'notifications', name: 'Bildirimler', icon: '🔔' },
    { id: 'business', name: 'İş Ayarları', icon: '🏢' }
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Ayarlar</h1>
          <p className="text-gray-600">Sistem ayarları ve konfigürasyon</p>
        </div>
        <button
          onClick={handleSave}
          disabled={saving}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
        >
          {saving ? 'Kaydediliyor...' : 'Kaydet'}
        </button>
      </div>

      <div className="flex space-x-6">
        {/* Sidebar */}
        <div className="w-64 bg-white rounded-lg shadow-sm border p-4">
          <nav className="space-y-2">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`w-full flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-colors ${
                  activeTab === tab.id
                    ? 'bg-blue-100 text-blue-700'
                    : 'text-gray-600 hover:bg-gray-100'
                }`}
              >
                <span className="mr-3">{tab.icon}</span>
                {tab.name}
              </button>
            ))}
          </nav>
        </div>

        {/* Content */}
        <div className="flex-1 bg-white rounded-lg shadow-sm border p-6">
          {activeTab === 'system' && (
            <div className="space-y-6">
              <h2 className="text-lg font-semibold text-gray-900">Sistem Ayarları</h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Uygulama Adı
                  </label>
                  <input
                    type="text"
                    value={settings.system.appName}
                    onChange={(e) => handleInputChange('system', 'appName', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Saat Dilimi
                  </label>
                  <select
                    value={settings.system.timezone}
                    onChange={(e) => handleInputChange('system', 'timezone', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                    <option value="Europe/Istanbul">Europe/Istanbul</option>
                    <option value="Europe/London">Europe/London</option>
                    <option value="America/New_York">America/New_York</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Para Birimi
                  </label>
                  <select
                    value={settings.system.currency}
                    onChange={(e) => handleInputChange('system', 'currency', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                    <option value="TRY">TRY (Türk Lirası)</option>
                    <option value="USD">USD (Amerikan Doları)</option>
                    <option value="EUR">EUR (Euro)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Dil
                  </label>
                  <select
                    value={settings.system.language}
                    onChange={(e) => handleInputChange('system', 'language', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                    <option value="tr">Türkçe</option>
                    <option value="en">English</option>
                  </select>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'whatsapp' && (
            <div className="space-y-6">
              <h2 className="text-lg font-semibold text-gray-900">WhatsApp Ayarları</h2>
              
              <div className="space-y-4">
                <div className="flex items-center">
                  <input
                    type="checkbox"
                    id="whatsappEnabled"
                    checked={settings.whatsapp.enabled}
                    onChange={(e) => handleInputChange('whatsapp', 'enabled', e.target.checked)}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label htmlFor="whatsappEnabled" className="ml-2 block text-sm text-gray-700">
                    WhatsApp entegrasyonunu etkinleştir
                  </label>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Webhook URL
                  </label>
                  <input
                    type="url"
                    value={settings.whatsapp.webhookUrl}
                    onChange={(e) => handleInputChange('whatsapp', 'webhookUrl', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    placeholder="https://your-domain.com/api/webhook/whatsapp"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Verify Token
                  </label>
                  <input
                    type="text"
                    value={settings.whatsapp.verifyToken}
                    onChange={(e) => handleInputChange('whatsapp', 'verifyToken', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>

                <div className="flex items-center">
                  <input
                    type="checkbox"
                    id="sandboxMode"
                    checked={settings.whatsapp.sandboxMode}
                    onChange={(e) => handleInputChange('whatsapp', 'sandboxMode', e.target.checked)}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label htmlFor="sandboxMode" className="ml-2 block text-sm text-gray-700">
                    Sandbox modu (test için)
                  </label>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'notifications' && (
            <div className="space-y-6">
              <h2 className="text-lg font-semibold text-gray-900">Bildirim Ayarları</h2>
              
              <div className="space-y-4">
                <div className="flex items-center">
                  <input
                    type="checkbox"
                    id="emailEnabled"
                    checked={settings.notifications.emailEnabled}
                    onChange={(e) => handleInputChange('notifications', 'emailEnabled', e.target.checked)}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label htmlFor="emailEnabled" className="ml-2 block text-sm text-gray-700">
                    E-posta bildirimleri
                  </label>
                </div>

                <div className="flex items-center">
                  <input
                    type="checkbox"
                    id="smsEnabled"
                    checked={settings.notifications.smsEnabled}
                    onChange={(e) => handleInputChange('notifications', 'smsEnabled', e.target.checked)}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label htmlFor="smsEnabled" className="ml-2 block text-sm text-gray-700">
                    SMS bildirimleri
                  </label>
                </div>

                <div className="flex items-center">
                  <input
                    type="checkbox"
                    id="whatsappEnabled"
                    checked={settings.notifications.whatsappEnabled}
                    onChange={(e) => handleInputChange('notifications', 'whatsappEnabled', e.target.checked)}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label htmlFor="whatsappEnabled" className="ml-2 block text-sm text-gray-700">
                    WhatsApp bildirimleri
                  </label>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Hatırlatma Süresi (saat)
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="168"
                    value={settings.notifications.reminderHours}
                    onChange={(e) => handleInputChange('notifications', 'reminderHours', parseInt(e.target.value))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
              </div>
            </div>
          )}

          {activeTab === 'business' && (
            <div className="space-y-6">
              <h2 className="text-lg font-semibold text-gray-900">İş Ayarları</h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Çalışma Saati Başlangıç
                  </label>
                  <input
                    type="time"
                    value={settings.business.workingHours.start}
                    onChange={(e) => handleNestedInputChange('business', 'workingHours', 'start', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Çalışma Saati Bitiş
                  </label>
                  <input
                    type="time"
                    value={settings.business.workingHours.end}
                    onChange={(e) => handleNestedInputChange('business', 'workingHours', 'end', e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Randevu Süresi (dakika)
                  </label>
                  <input
                    type="number"
                    min="15"
                    max="120"
                    step="15"
                    value={settings.business.appointmentDuration}
                    onChange={(e) => handleInputChange('business', 'appointmentDuration', parseInt(e.target.value))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Maksimum İleri Tarih (gün)
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="365"
                    value={settings.business.maxAdvanceDays}
                    onChange={(e) => handleInputChange('business', 'maxAdvanceDays', parseInt(e.target.value))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Çalışma Günleri
                </label>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
                  {[
                    { key: 'monday', label: 'Pazartesi' },
                    { key: 'tuesday', label: 'Salı' },
                    { key: 'wednesday', label: 'Çarşamba' },
                    { key: 'thursday', label: 'Perşembe' },
                    { key: 'friday', label: 'Cuma' },
                    { key: 'saturday', label: 'Cumartesi' },
                    { key: 'sunday', label: 'Pazar' }
                  ].map((day) => (
                    <label key={day.key} className="flex items-center">
                      <input
                        type="checkbox"
                        checked={settings.business.workingDays.includes(day.key)}
                        onChange={(e) => {
                          const newDays = e.target.checked
                            ? [...settings.business.workingDays, day.key]
                            : settings.business.workingDays.filter(d => d !== day.key);
                          handleInputChange('business', 'workingDays', newDays);
                        }}
                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                      />
                      <span className="ml-2 text-sm text-gray-700">{day.label}</span>
                    </label>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Settings;
