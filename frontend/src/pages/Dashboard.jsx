import React, { useState, useEffect } from 'react';
import { 
  Users, 
  Store, 
  Calendar, 
  TrendingUp,
  Clock,
  CheckCircle,
  XCircle,
  BarChart3
} from 'lucide-react';
import { adminAPI } from '../utils/api';

const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchDashboardStats();
  }, []);

  const fetchDashboardStats = async () => {
    try {
      setLoading(true);
      const data = await adminAPI.getDashboardStats();
      setStats(data);
    } catch (error) {
      console.error('Dashboard stats fetch failed:', error);
      setError('İstatistikler yüklenirken hata oluştu');
    } finally {
      setLoading(false);
    }
  };

  const StatCard = ({ title, value, subtitle, icon: Icon, color = 'blue', trend }) => (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-gray-600">{title}</p>
          <p className="text-2xl font-bold text-gray-900 mt-1">{value}</p>
          {subtitle && (
            <p className="text-sm text-gray-500 mt-1">{subtitle}</p>
          )}
          {trend && (
            <div className="flex items-center mt-2">
              <TrendingUp className={`h-4 w-4 mr-1 ${
                trend > 0 ? 'text-green-500' : 'text-red-500'
              }`} />
              <span className={`text-sm font-medium ${
                trend > 0 ? 'text-green-600' : 'text-red-600'
              }`}>
                {trend > 0 ? '+' : ''}{trend}%
              </span>
            </div>
          )}
        </div>
        <div className={`p-3 rounded-full bg-${color}-100`}>
          <Icon className={`h-6 w-6 text-${color}-600`} />
        </div>
      </div>
    </div>
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="spinner"></div>
        <span className="ml-2 text-gray-600">İstatistikler yükleniyor...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4">
        <div className="flex items-center">
          <XCircle className="h-5 w-5 text-red-500 mr-2" />
          <span className="text-red-800">{error}</span>
        </div>
        <button
          onClick={fetchDashboardStats}
          className="mt-2 text-sm text-red-600 hover:text-red-800 underline"
        >
          Tekrar dene
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-gray-600 mt-1">
            Sistem geneli istatistikler ve özet bilgiler
          </p>
        </div>
        <div className="flex items-center space-x-2 text-sm text-gray-500">
          <Clock className="h-4 w-4" />
          <span>Son güncelleme: {new Date(stats?.lastUpdated).toLocaleString('tr-TR')}</span>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Toplam Kuaför"
          value={stats?.totalTenants || 0}
          subtitle={`${stats?.activeTenants || 0} aktif`}
          icon={Store}
          color="blue"
        />
        
        <StatCard
          title="Toplam Müşteri"
          value={stats?.totalCustomers || 0}
          subtitle="Tüm kuaförlerde"
          icon={Users}
          color="green"
        />
        
        <StatCard
          title="Toplam Randevu"
          value={stats?.totalAppointments || 0}
          subtitle="Tüm zamanlar"
          icon={Calendar}
          color="purple"
        />
        
        <StatCard
          title="Bugünkü Randevular"
          value={stats?.todayAppointments || 0}
          subtitle="Aktif randevular"
          icon={CheckCircle}
          color="orange"
        />
      </div>

      {/* Additional Info */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Bu Ay İstatistikleri */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900">Bu Ay</h3>
            <BarChart3 className="h-5 w-5 text-gray-400" />
          </div>
          
          <div className="space-y-4">
            <div className="flex justify-between items-center">
              <span className="text-gray-600">Aylık Randevular</span>
              <span className="font-semibold text-gray-900">
                {stats?.monthlyAppointments || 0}
              </span>
            </div>
            
            <div className="flex justify-between items-center">
              <span className="text-gray-600">Aktif Kuaför Oranı</span>
              <span className="font-semibold text-gray-900">
                {stats?.totalTenants > 0 
                  ? Math.round((stats?.activeTenants / stats?.totalTenants) * 100) 
                  : 0}%
              </span>
            </div>
            
            <div className="flex justify-between items-center">
              <span className="text-gray-600">Ortalama Günlük Randevu</span>
              <span className="font-semibold text-gray-900">
                {stats?.monthlyAppointments > 0 
                  ? Math.round(stats?.monthlyAppointments / 30) 
                  : 0}
              </span>
            </div>
          </div>
        </div>

        {/* Sistem Durumu */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900">Sistem Durumu</h3>
            <CheckCircle className="h-5 w-5 text-green-500" />
          </div>
          
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-gray-600">Backend API</span>
              <span className="flex items-center text-green-600">
                <div className="w-2 h-2 bg-green-500 rounded-full mr-2"></div>
                Aktif
              </span>
            </div>
            
            <div className="flex items-center justify-between">
              <span className="text-gray-600">Veritabanı</span>
              <span className="flex items-center text-green-600">
                <div className="w-2 h-2 bg-green-500 rounded-full mr-2"></div>
                Bağlı
              </span>
            </div>
            
            <div className="flex items-center justify-between">
              <span className="text-gray-600">WhatsApp API</span>
              <span className="flex items-center text-green-600">
                <div className="w-2 h-2 bg-green-500 rounded-full mr-2"></div>
                Hazır
              </span>
            </div>
            
            <div className="flex items-center justify-between">
              <span className="text-gray-600">Bildirimler</span>
              <span className="flex items-center text-green-600">
                <div className="w-2 h-2 bg-green-500 rounded-full mr-2"></div>
                Çalışıyor
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Hızlı İşlemler</h3>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <button className="flex items-center p-4 bg-blue-50 hover:bg-blue-100 rounded-lg transition-colors">
            <Store className="h-8 w-8 text-blue-600 mr-3" />
            <div className="text-left">
              <div className="font-medium text-blue-900">Yeni Kuaför Ekle</div>
              <div className="text-sm text-blue-700">Sisteme yeni kuaför kaydet</div>
            </div>
          </button>
          
          <button className="flex items-center p-4 bg-green-50 hover:bg-green-100 rounded-lg transition-colors">
            <BarChart3 className="h-8 w-8 text-green-600 mr-3" />
            <div className="text-left">
              <div className="font-medium text-green-900">Raporları Görüntüle</div>
              <div className="text-sm text-green-700">Detaylı istatistikleri incele</div>
            </div>
          </button>
          
          <button className="flex items-center p-4 bg-purple-50 hover:bg-purple-100 rounded-lg transition-colors">
            <Users className="h-8 w-8 text-purple-600 mr-3" />
            <div className="text-left">
              <div className="font-medium text-purple-900">Kuaför Listesi</div>
              <div className="text-sm text-purple-700">Tüm kuaförleri yönet</div>
            </div>
          </button>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
