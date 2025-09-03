import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  Users, 
  Store, 
  Calendar, 
  TrendingUp,
  Clock,
  CheckCircle,
  XCircle,
  BarChart3,
  UserCheck,
  Scissors,
  DollarSign,
  Activity,
  Star,
  Zap,
  ArrowUpRight,
  ArrowDownRight,
  Eye,
  Target,
  Award,
  Sparkles
} from 'lucide-react';
import api from '../utils/api';

const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [recentAppointments, setRecentAppointments] = useState([]);
  const [topBarbers, setTopBarbers] = useState([]);
  const [weeklyData, setWeeklyData] = useState([]);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      
      // Paralel olarak tüm verileri çek
      const [tenantsRes, customersRes, appointmentsRes] = await Promise.all([
        api.get('/tenants'),
        api.get('/customers'),
        api.get('/appointments')
      ]);

      const tenants = tenantsRes.data || [];
      const customers = customersRes.data || [];
      const appointments = appointmentsRes.data || [];

      // İstatistikleri hesapla
      const today = new Date().toISOString().split('T')[0];
      const todayAppointments = appointments.filter(apt => 
        apt.startTime && apt.startTime.startsWith(today)
      );

      const thisMonth = new Date().getMonth();
      const thisYear = new Date().getFullYear();
      const monthlyAppointments = appointments.filter(apt => {
        if (!apt.startTime) return false;
        const aptDate = new Date(apt.startTime);
        return aptDate.getMonth() === thisMonth && aptDate.getFullYear() === thisYear;
      });

      // Toplam gelir hesapla
      const totalRevenue = appointments.reduce((sum, apt) => sum + (apt.totalPrice || 0), 0);
      const monthlyRevenue = monthlyAppointments.reduce((sum, apt) => sum + (apt.totalPrice || 0), 0);

      // En aktif kuaförleri hesapla
      const barberStats = {};
      appointments.forEach(apt => {
        if (apt.tenantId) {
          barberStats[apt.tenantId] = (barberStats[apt.tenantId] || 0) + 1;
        }
      });

      const topBarbersList = Object.entries(barberStats)
        .map(([tenantId, count]) => {
          const tenant = tenants.find(t => t.id === parseInt(tenantId));
          return { ...tenant, appointmentCount: count };
        })
        .sort((a, b) => b.appointmentCount - a.appointmentCount)
        .slice(0, 5);

      // Son randevular
      const recentAppts = appointments
        .sort((a, b) => new Date(b.startTime) - new Date(a.startTime))
        .slice(0, 5);

      // Haftalık veri (son 7 gün)
      const weeklyDataList = [];
      for (let i = 6; i >= 0; i--) {
        const date = new Date();
        date.setDate(date.getDate() - i);
        const dateStr = date.toISOString().split('T')[0];
        const dayAppointments = appointments.filter(apt => 
          apt.startTime && apt.startTime.startsWith(dateStr)
        );
        weeklyDataList.push({
          date: dateStr,
          count: dayAppointments.length,
          revenue: dayAppointments.reduce((sum, apt) => sum + (apt.totalPrice || 0), 0)
        });
      }

      setStats({
        totalTenants: tenants.length,
        activeTenants: tenants.filter(t => t.active).length,
        totalCustomers: customers.length,
        totalAppointments: appointments.length,
        todayAppointments: todayAppointments.length,
        monthlyAppointments: monthlyAppointments.length,
        totalRevenue,
        monthlyRevenue,
        lastUpdated: new Date().toISOString()
      });

      setRecentAppointments(recentAppts);
      setTopBarbers(topBarbersList);
      setWeeklyData(weeklyDataList);

    } catch (error) {
      console.error('Dashboard data fetch failed:', error);
      setError('Veriler yüklenirken hata oluştu');
    } finally {
      setLoading(false);
    }
  };

  const StatCard = ({ title, value, subtitle, icon: Icon, color = 'blue', trend, gradient, sparkle }) => (
    <div className={`relative bg-white rounded-2xl shadow-lg border border-gray-100 p-6 hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1 ${gradient ? 'bg-gradient-to-br from-white to-gray-50' : ''}`}>
      {sparkle && (
        <div className="absolute top-4 right-4">
          <Sparkles className="h-4 w-4 text-yellow-400 animate-pulse" />
        </div>
      )}
      <div className="flex items-center justify-between">
        <div className="flex-1">
          <p className="text-sm font-medium text-gray-600 mb-1">{title}</p>
          <p className="text-3xl font-bold text-gray-900 mb-2">{value}</p>
          {subtitle && (
            <p className="text-sm text-gray-500 mb-2">{subtitle}</p>
          )}
          {trend && (
            <div className="flex items-center">
              {trend > 0 ? (
                <ArrowUpRight className="h-4 w-4 text-green-500 mr-1" />
              ) : (
                <ArrowDownRight className="h-4 w-4 text-red-500 mr-1" />
              )}
              <span className={`text-sm font-semibold ${
                trend > 0 ? 'text-green-600' : 'text-red-600'
              }`}>
                {trend > 0 ? '+' : ''}{trend}%
              </span>
              <span className="text-xs text-gray-500 ml-1">bu ay</span>
            </div>
          )}
        </div>
        <div className={`p-4 rounded-2xl bg-gradient-to-br ${
          color === 'blue' ? 'from-blue-500 to-blue-600' :
          color === 'green' ? 'from-green-500 to-green-600' :
          color === 'purple' ? 'from-purple-500 to-purple-600' :
          color === 'orange' ? 'from-orange-500 to-orange-600' :
          color === 'pink' ? 'from-pink-500 to-pink-600' :
          'from-gray-500 to-gray-600'
        } shadow-lg`}>
          <Icon className="h-6 w-6 text-white" />
        </div>
      </div>
    </div>
  );

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('tr-TR', {
      style: 'currency',
      currency: 'TRY'
    }).format(amount);
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('tr-TR', {
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="relative">
            <div className="w-16 h-16 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin mx-auto"></div>
            <div className="absolute inset-0 flex items-center justify-center">
              <Sparkles className="w-6 h-6 text-blue-600 animate-pulse" />
            </div>
          </div>
          <p className="mt-4 text-lg font-medium text-gray-600">Dashboard yükleniyor...</p>
          <p className="text-sm text-gray-500">Verileriniz hazırlanıyor</p>
        </div>
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
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-4xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
            Dashboard
          </h1>
          <p className="text-gray-600 mt-2 text-lg">
            Sistem geneli istatistikler ve performans analizi
          </p>
        </div>
        <div className="flex items-center space-x-3 bg-white px-4 py-2 rounded-xl shadow-sm border border-gray-100">
          <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
          <Clock className="h-4 w-4 text-gray-500" />
          <span className="text-sm text-gray-600">
            {new Date().toLocaleString('tr-TR')}
          </span>
        </div>
      </div>

      {/* Premium Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Toplam Kuaför"
          value={stats?.totalTenants || 0}
          subtitle={`${stats?.activeTenants || 0} aktif`}
          icon={Store}
          color="blue"
          trend={12}
          sparkle={true}
        />
        
        <StatCard
          title="Toplam Müşteri"
          value={stats?.totalCustomers || 0}
          subtitle="Kayıtlı müşteriler"
          icon={Users}
          color="green"
          trend={8}
        />
        
        <StatCard
          title="Toplam Randevu"
          value={stats?.totalAppointments || 0}
          subtitle="Tüm zamanlar"
          icon={Calendar}
          color="purple"
          trend={15}
        />
        
        <StatCard
          title="Toplam Gelir"
          value={formatCurrency(stats?.totalRevenue || 0)}
          subtitle="Tüm zamanlar"
          icon={DollarSign}
          color="pink"
          trend={22}
          sparkle={true}
        />
      </div>

      {/* Secondary Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <StatCard
          title="Bugünkü Randevular"
          value={stats?.todayAppointments || 0}
          subtitle="Bugün için planlanan"
          icon={Target}
          color="orange"
        />
        
        <StatCard
          title="Bu Ay Randevular"
          value={stats?.monthlyAppointments || 0}
          subtitle="Aylık performans"
          icon={Activity}
          color="blue"
        />
        
        <StatCard
          title="Aylık Gelir"
          value={formatCurrency(stats?.monthlyRevenue || 0)}
          subtitle="Bu ay kazanılan"
          icon={Award}
          color="green"
        />
      </div>

      {/* Charts and Analytics */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Haftalık Performans Grafiği */}
        <div className="bg-white rounded-2xl shadow-lg border border-gray-100 p-6">
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-xl font-bold text-gray-900">Haftalık Performans</h3>
            <BarChart3 className="h-6 w-6 text-blue-600" />
          </div>
          
          <div className="space-y-4">
            {weeklyData.map((day, index) => (
              <div key={index} className="flex items-center justify-between">
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
                    <span className="text-xs font-semibold text-blue-600">
                      {new Date(day.date).getDate()}
                    </span>
                  </div>
                  <span className="text-sm font-medium text-gray-700">
                    {new Date(day.date).toLocaleDateString('tr-TR', { weekday: 'short' })}
                  </span>
                </div>
                <div className="flex items-center space-x-4">
                  <div className="text-right">
                    <div className="text-sm font-semibold text-gray-900">{day.count} randevu</div>
                    <div className="text-xs text-gray-500">{formatCurrency(day.revenue)}</div>
                  </div>
                  <div className="w-20 bg-gray-200 rounded-full h-2">
                    <div 
                      className="bg-gradient-to-r from-blue-500 to-purple-500 h-2 rounded-full transition-all duration-500"
                      style={{ width: `${Math.min((day.count / Math.max(...weeklyData.map(d => d.count))) * 100, 100)}%` }}
                    ></div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* En Aktif Kuaförler */}
        <div className="bg-white rounded-2xl shadow-lg border border-gray-100 p-6">
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-xl font-bold text-gray-900">En Aktif Kuaförler</h3>
            <Star className="h-6 w-6 text-yellow-500" />
          </div>
          
          <div className="space-y-4">
            {topBarbers.length > 0 ? (
              topBarbers.map((barber, index) => (
                <div key={barber.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-xl">
                  <div className="flex items-center space-x-3">
                    <div className={`w-8 h-8 rounded-full flex items-center justify-center text-white font-bold text-sm ${
                      index === 0 ? 'bg-gradient-to-r from-yellow-400 to-yellow-500' :
                      index === 1 ? 'bg-gradient-to-r from-gray-400 to-gray-500' :
                      index === 2 ? 'bg-gradient-to-r from-orange-400 to-orange-500' :
                      'bg-gradient-to-r from-blue-400 to-blue-500'
                    }`}>
                      {index + 1}
                    </div>
                    <div>
                      <div className="font-semibold text-gray-900">{barber.name || 'Bilinmeyen'}</div>
                      <div className="text-xs text-gray-500">{barber.phoneNumber}</div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="font-bold text-gray-900">{barber.appointmentCount}</div>
                    <div className="text-xs text-gray-500">randevu</div>
                  </div>
                </div>
              ))
            ) : (
              <div className="text-center py-8 text-gray-500">
                <Store className="h-12 w-12 mx-auto mb-2 text-gray-300" />
                <p>Henüz randevu verisi yok</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Son Randevular */}
      <div className="bg-white rounded-2xl shadow-lg border border-gray-100 p-6">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-xl font-bold text-gray-900">Son Randevular</h3>
          <Link 
            to="/appointments" 
            className="text-blue-600 hover:text-blue-800 text-sm font-medium flex items-center"
          >
            Tümünü Gör
            <ArrowUpRight className="h-4 w-4 ml-1" />
          </Link>
        </div>
        
        <div className="space-y-3">
          {recentAppointments.length > 0 ? (
            recentAppointments.map((appointment) => (
              <div key={appointment.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-xl hover:bg-gray-100 transition-colors">
                <div className="flex items-center space-x-4">
                  <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                    <Calendar className="h-5 w-5 text-blue-600" />
                  </div>
                  <div>
                    <div className="font-semibold text-gray-900">
                      {appointment.customerName || 'Bilinmeyen Müşteri'}
                    </div>
                    <div className="text-sm text-gray-500">
                      {appointment.serviceName || 'Hizmet'} • {formatCurrency(appointment.totalPrice || 0)}
                    </div>
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-sm font-medium text-gray-900">
                    {formatDate(appointment.startTime)}
                  </div>
                  <div className={`text-xs px-2 py-1 rounded-full ${
                    appointment.status === 'CONFIRMED' ? 'bg-green-100 text-green-800' :
                    appointment.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                    appointment.status === 'COMPLETED' ? 'bg-blue-100 text-blue-800' :
                    'bg-gray-100 text-gray-800'
                  }`}>
                    {appointment.status === 'CONFIRMED' ? 'Onaylandı' :
                     appointment.status === 'PENDING' ? 'Beklemede' :
                     appointment.status === 'COMPLETED' ? 'Tamamlandı' :
                     appointment.status}
                  </div>
                </div>
              </div>
            ))
          ) : (
            <div className="text-center py-8 text-gray-500">
              <Calendar className="h-12 w-12 mx-auto mb-2 text-gray-300" />
              <p>Henüz randevu yok</p>
            </div>
          )}
        </div>
      </div>

      {/* Premium Quick Actions */}
      <div className="bg-gradient-to-br from-white to-gray-50 rounded-2xl shadow-lg border border-gray-100 p-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h3 className="text-2xl font-bold text-gray-900">Hızlı İşlemler</h3>
            <p className="text-gray-600 mt-1">En sık kullanılan işlemler</p>
          </div>
          <Zap className="h-8 w-8 text-yellow-500" />
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <Link to="/tenants/create" className="group flex items-center p-6 bg-gradient-to-br from-blue-50 to-blue-100 hover:from-blue-100 hover:to-blue-200 rounded-2xl transition-all duration-300 transform hover:-translate-y-1 hover:shadow-lg">
            <div className="p-3 bg-blue-500 rounded-xl group-hover:scale-110 transition-transform">
              <Store className="h-6 w-6 text-white" />
            </div>
            <div className="ml-4 text-left">
              <div className="font-semibold text-blue-900">Yeni Kuaför</div>
              <div className="text-sm text-blue-700">Sisteme ekle</div>
            </div>
          </Link>
          
          <Link to="/appointments" className="group flex items-center p-6 bg-gradient-to-br from-green-50 to-green-100 hover:from-green-100 hover:to-green-200 rounded-2xl transition-all duration-300 transform hover:-translate-y-1 hover:shadow-lg">
            <div className="p-3 bg-green-500 rounded-xl group-hover:scale-110 transition-transform">
              <Calendar className="h-6 w-6 text-white" />
            </div>
            <div className="ml-4 text-left">
              <div className="font-semibold text-green-900">Randevular</div>
              <div className="text-sm text-green-700">Yönet ve görüntüle</div>
            </div>
          </Link>
          
          <Link to="/customers" className="group flex items-center p-6 bg-gradient-to-br from-purple-50 to-purple-100 hover:from-purple-100 hover:to-purple-200 rounded-2xl transition-all duration-300 transform hover:-translate-y-1 hover:shadow-lg">
            <div className="p-3 bg-purple-500 rounded-xl group-hover:scale-110 transition-transform">
              <Users className="h-6 w-6 text-white" />
            </div>
            <div className="ml-4 text-left">
              <div className="font-semibold text-purple-900">Müşteriler</div>
              <div className="text-sm text-purple-700">Müşteri yönetimi</div>
            </div>
          </Link>
          
          <Link to="/reports" className="group flex items-center p-6 bg-gradient-to-br from-orange-50 to-orange-100 hover:from-orange-100 hover:to-orange-200 rounded-2xl transition-all duration-300 transform hover:-translate-y-1 hover:shadow-lg">
            <div className="p-3 bg-orange-500 rounded-xl group-hover:scale-110 transition-transform">
              <BarChart3 className="h-6 w-6 text-white" />
            </div>
            <div className="ml-4 text-left">
              <div className="font-semibold text-orange-900">Raporlar</div>
              <div className="text-sm text-orange-700">Analiz ve raporlar</div>
            </div>
          </Link>
        </div>
      </div>

      {/* Sistem Durumu */}
      <div className="bg-white rounded-2xl shadow-lg border border-gray-100 p-6">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-xl font-bold text-gray-900">Sistem Durumu</h3>
          <div className="flex items-center space-x-2">
            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
            <span className="text-sm font-medium text-green-600">Tüm Sistemler Aktif</span>
          </div>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="flex items-center justify-between p-4 bg-green-50 rounded-xl">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 bg-green-500 rounded-full flex items-center justify-center">
                <CheckCircle className="h-5 w-5 text-white" />
              </div>
              <div>
                <div className="font-semibold text-gray-900">Backend API</div>
                <div className="text-sm text-gray-500">Çalışıyor</div>
              </div>
            </div>
            <div className="w-2 h-2 bg-green-500 rounded-full"></div>
          </div>
          
          <div className="flex items-center justify-between p-4 bg-green-50 rounded-xl">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 bg-green-500 rounded-full flex items-center justify-center">
                <Activity className="h-5 w-5 text-white" />
              </div>
              <div>
                <div className="font-semibold text-gray-900">Veritabanı</div>
                <div className="text-sm text-gray-500">Bağlı</div>
              </div>
            </div>
            <div className="w-2 h-2 bg-green-500 rounded-full"></div>
          </div>
          
          <div className="flex items-center justify-between p-4 bg-green-50 rounded-xl">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 bg-green-500 rounded-full flex items-center justify-center">
                <Zap className="h-5 w-5 text-white" />
              </div>
              <div>
                <div className="font-semibold text-gray-900">WhatsApp API</div>
                <div className="text-sm text-gray-500">Hazır</div>
              </div>
            </div>
            <div className="w-2 h-2 bg-green-500 rounded-full"></div>
          </div>
          
          <div className="flex items-center justify-between p-4 bg-green-50 rounded-xl">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 bg-green-500 rounded-full flex items-center justify-center">
                <Eye className="h-5 w-5 text-white" />
              </div>
              <div>
                <div className="font-semibold text-gray-900">Bildirimler</div>
                <div className="text-sm text-gray-500">Aktif</div>
              </div>
            </div>
            <div className="w-2 h-2 bg-green-500 rounded-full"></div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
