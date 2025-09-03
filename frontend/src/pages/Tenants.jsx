import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Search, Edit, Trash2, Eye, MoreHorizontal, Users, BarChart3 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const Tenants = () => {
  const { token } = useAuth();
  const [tenants, setTenants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filteredTenants, setFilteredTenants] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchTenants();
  }, []);

  useEffect(() => {
    if (Array.isArray(tenants)) {
      if (searchTerm.trim() === '') {
        setFilteredTenants(tenants);
      } else {
        const filtered = tenants.filter(tenant =>
          tenant.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
          tenant.phoneNumber.includes(searchTerm) ||
          tenant.email?.toLowerCase().includes(searchTerm.toLowerCase())
        );
        setFilteredTenants(filtered);
      }
    } else {
      setFilteredTenants([]);
    }
  }, [searchTerm, tenants]);

  const fetchTenants = async () => {
    try {
      setError(null);
      const response = await fetch('http://localhost:8080/api/admin/tenants', {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const data = await response.json();
        // API'den gelen veriyi kontrol et
        if (Array.isArray(data)) {
          setTenants(data);
        } else {
          console.error('API unexpected data format:', data);
          setError('Veri formatı beklenenden farklı');
          setTenants([]);
        }
      } else {
        console.error('Tenants fetch failed:', response.status);
        setError(`API hatası: ${response.status}`);
        setTenants([]);
      }
    } catch (error) {
      console.error('Error fetching tenants:', error);
      setError('Bağlantı hatası');
      setTenants([]);
    } finally {
      setLoading(false);
    }
  };

  const toggleTenantStatus = async (tenantId, currentStatus) => {
    try {
      const response = await fetch(`http://localhost:8080/api/admin/tenants/${tenantId}/toggle-status`, {
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        // Update local state
        setTenants(prev => prev.map(tenant =>
          tenant.id === tenantId
            ? { ...tenant, active: !currentStatus }
            : tenant
        ));
      } else {
        console.error('Status toggle failed:', response.status);
      }
    } catch (error) {
      console.error('Error toggling status:', error);
    }
  };

  const deleteTenant = async (tenantId) => {
    if (!window.confirm('Bu kuaförü silmek istediğinizden emin misiniz?')) {
      return;
    }

    try {
      const response = await fetch(`http://localhost:8080/api/admin/tenants/${tenantId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        setTenants(prev => prev.filter(tenant => tenant.id !== tenantId));
      } else {
        console.error('Delete failed:', response.status);
      }
    } catch (error) {
      console.error('Error deleting tenant:', error);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <div className="spinner mx-auto mb-4"></div>
          <p className="text-gray-600">Yükleniyor...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Kuaförler</h1>
          <p className="text-gray-600">Sistemdeki tüm kuaförleri yönetin</p>
        </div>
        <Link
          to="/tenants/create"
          className="inline-flex items-center px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus className="w-4 h-4 mr-2" />
          Yeni Kuaför Ekle
        </Link>
      </div>

      {/* Search and Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <div className="flex items-center">
            <div className="p-2 bg-blue-100 rounded-lg">
              <Users className="w-6 h-6 text-blue-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Toplam Kuaför</p>
              <p className="text-2xl font-bold text-gray-900">{Array.isArray(tenants) ? tenants.length : 0}</p>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <div className="flex items-center">
            <div className="p-2 bg-green-100 rounded-lg">
              <Eye className="w-6 h-6 text-green-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Aktif Kuaför</p>
              <p className="text-2xl font-bold text-gray-900">
                {Array.isArray(tenants) ? tenants.filter(t => t.active).length : 0}
              </p>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <div className="flex items-center">
            <div className="p-2 bg-yellow-100 rounded-lg">
              <BarChart3 className="w-6 h-6 text-yellow-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Pasif Kuaför</p>
              <p className="text-2xl font-bold text-gray-900">
                {Array.isArray(tenants) ? tenants.filter(t => !t.active).length : 0}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="flex">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-red-800">Hata</h3>
              <div className="mt-2 text-sm text-red-700">
                <p>{error}</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Search Bar */}
      <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
          <input
            type="text"
            placeholder="Kuaför ara (isim, telefon, email)..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>
      </div>

      {/* Tenants Table */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Kuaför
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  İletişim
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Adres
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Durum
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Kayıt Tarihi
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  İşlemler
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredTenants.map((tenant) => (
                <tr key={tenant.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div className="flex-shrink-0 h-10 w-10">
                        {tenant.logoUrl ? (
                          <img
                            className="h-10 w-10 rounded-full object-cover"
                            src={tenant.logoUrl}
                            alt={tenant.name}
                          />
                        ) : (
                          <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center">
                            <Users className="w-5 h-5 text-blue-600" />
                          </div>
                        )}
                      </div>
                      <div className="ml-4">
                        <div className="text-sm font-medium text-gray-900">
                          {tenant.name}
                        </div>
                        <div className="text-sm text-gray-500">
                          ID: {tenant.id}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">
                      {tenant.phoneNumber}
                    </div>
                    {tenant.email && (
                      <div className="text-sm text-gray-500">
                        {tenant.email}
                      </div>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">
                      {tenant.address || 'Adres girilmemiş'}
                    </div>
                    <div className="text-sm text-gray-500">
                      {tenant.timezone || 'Europe/Istanbul'}
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                      tenant.active
                        ? 'bg-green-100 text-green-800'
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {tenant.active ? 'Aktif' : 'Pasif'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {tenant.createdAt ? new Date(tenant.createdAt).toLocaleDateString('tr-TR') : 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div className="flex items-center justify-end space-x-2">
                      <button
                        onClick={() => toggleTenantStatus(tenant.id, tenant.active)}
                        className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${
                          tenant.active
                            ? 'text-red-700 bg-red-100 hover:bg-red-200'
                            : 'text-green-700 bg-green-100 hover:bg-green-200'
                        }`}
                      >
                        {tenant.active ? 'Pasif Yap' : 'Aktif Yap'}
                      </button>
                      <Link
                        to={`/tenants/${tenant.id}/edit`}
                        className="text-blue-600 hover:text-blue-900 px-3 py-1 text-xs font-medium rounded-md bg-blue-100 hover:bg-blue-200 transition-colors"
                      >
                        <Edit className="w-4 h-4" />
                      </Link>
                      <button
                        onClick={() => deleteTenant(tenant.id)}
                        className="text-red-600 hover:text-red-900 px-3 py-1 text-xs font-medium rounded-md bg-red-100 hover:bg-red-200 transition-colors"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {filteredTenants.length === 0 && (
          <div className="text-center py-12">
            <Users className="mx-auto h-12 w-12 text-gray-400" />
            <h3 className="mt-2 text-sm font-medium text-gray-900">
              {searchTerm ? 'Arama sonucu bulunamadı' : 'Henüz kuaför eklenmemiş'}
            </h3>
            <p className="mt-1 text-sm text-gray-500">
              {searchTerm ? 'Farklı arama terimleri deneyin' : 'İlk kuaförü eklemek için yukarıdaki butonu kullanın'}
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default Tenants;
