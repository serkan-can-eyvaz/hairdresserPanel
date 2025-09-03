import React from 'react';
import { NavLink } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Users, 
  UserPlus, 
  BarChart3, 
  CreditCard,
  Store,
  Settings,
  Scissors,
  Calendar,
  UserCheck
} from 'lucide-react';

const Sidebar = () => {
  const navigation = [
    {
      name: 'Dashboard',
      href: '/dashboard',
      icon: LayoutDashboard,
      description: 'Genel özet ve istatistikler'
    },
    {
      name: 'Kuaförler',
      href: '/tenants',
      icon: Store,
      description: 'Kuaför listesi ve yönetimi'
    },
    {
      name: 'Yeni Kuaför',
      href: '/tenants/create',
      icon: UserPlus,
      description: 'Yeni kuaför ekle'
    },
    {
      name: 'Hizmetler',
      href: '/services',
      icon: Scissors,
      description: 'Hizmet yönetimi'
    },
    {
      name: 'Randevular',
      href: '/appointments',
      icon: Calendar,
      description: 'Randevu yönetimi'
    },
    {
      name: 'Müşteriler',
      href: '/customers',
      icon: UserCheck,
      description: 'Müşteri yönetimi'
    },
    {
      name: 'Raporlar',
      href: '/reports',
      icon: BarChart3,
      description: 'İstatistikler ve raporlar'
    },
    {
      name: 'Abonelikler',
      href: '/subscriptions',
      icon: CreditCard,
      description: 'Abonelik yönetimi'
    },
    {
      name: 'Ayarlar',
      href: '/settings',
      icon: Settings,
      description: 'Sistem ayarları'
    }
  ];

  return (
    <div className="flex flex-col w-64 bg-white shadow-sm border-r border-gray-200">
      {/* Sidebar Header */}
      <div className="flex items-center justify-center h-16 px-4 border-b border-gray-200">
        <div className="flex items-center space-x-2">
          <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
            <Users className="w-4 h-4 text-white" />
          </div>
          <span className="text-lg font-semibold text-gray-900">Admin Panel</span>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-4 space-y-1">
        {navigation.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              key={item.name}
              to={item.href}
              className={({ isActive }) =>
                `group flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-all duration-200 ${
                  isActive
                    ? 'bg-blue-50 text-blue-700 border-r-2 border-blue-700'
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                }`
              }
            >
              {({ isActive }) => (
                <>
                  <Icon
                    className={`mr-3 h-5 w-5 transition-colors ${
                      isActive ? 'text-blue-700' : 'text-gray-400 group-hover:text-gray-500'
                    }`}
                  />
                  <div className="flex-1">
                    <div className="text-sm font-medium">{item.name}</div>
                    <div className={`text-xs ${
                      isActive ? 'text-blue-600' : 'text-gray-500'
                    }`}>
                      {item.description}
                    </div>
                  </div>
                </>
              )}
            </NavLink>
          );
        })}
      </nav>

      {/* Sidebar Footer */}
      <div className="px-3 py-4 border-t border-gray-200">
        <div className="bg-blue-50 rounded-lg p-3">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <div className="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center">
                <BarChart3 className="w-4 h-4 text-white" />
              </div>
            </div>
            <div className="ml-3 flex-1">
              <p className="text-sm font-medium text-blue-900">
                Sistem Durumu
              </p>
              <p className="text-xs text-blue-700">
                Tüm servisler aktif
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Sidebar;
