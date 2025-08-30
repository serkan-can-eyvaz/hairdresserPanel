import React from 'react';
import { useAuth } from '../context/AuthContext';
import { LogOut, User, Bell, Settings } from 'lucide-react';

const Navbar = () => {
  const { user, logout } = useAuth();

  const handleLogout = () => {
    if (window.confirm('Çıkış yapmak istediğinizden emin misiniz?')) {
      logout();
    }
  };

  return (
    <nav className="bg-white shadow-sm border-b border-gray-200">
      <div className="px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Sol taraf - Logo */}
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <h1 className="text-xl font-bold text-gray-900">
                Kuaför Panel
              </h1>
            </div>
          </div>

          {/* Sağ taraf - User menu */}
          <div className="flex items-center space-x-4">
            {/* Bildirimler */}
            <button className="p-2 text-gray-400 hover:text-gray-500 hover:bg-gray-100 rounded-lg transition-colors">
              <Bell className="h-5 w-5" />
            </button>

            {/* Ayarlar */}
            <button className="p-2 text-gray-400 hover:text-gray-500 hover:bg-gray-100 rounded-lg transition-colors">
              <Settings className="h-5 w-5" />
            </button>

            {/* User Dropdown */}
            <div className="relative flex items-center space-x-3">
              {/* User Avatar */}
              <div className="h-8 w-8 bg-blue-600 rounded-full flex items-center justify-center">
                <User className="h-4 w-4 text-white" />
              </div>
              
              {/* User Info */}
              <div className="hidden md:block text-sm">
                <div className="font-medium text-gray-900">
                  {user?.username}
                </div>
                <div className="text-gray-500">
                  {user?.role === 'SUPER_ADMIN' ? 'Super Admin' : 'Admin'}
                </div>
              </div>

              {/* Logout Button */}
              <button
                onClick={handleLogout}
                className="flex items-center space-x-2 px-3 py-2 text-sm text-gray-700 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                title="Çıkış Yap"
              >
                <LogOut className="h-4 w-4" />
                <span className="hidden sm:inline">Çıkış</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
