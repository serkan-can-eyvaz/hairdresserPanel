import axios from 'axios';

// API base URL - backend'in çalıştığı port
const API_BASE_URL = 'http://localhost:8080/api';

// Axios instance oluştur
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - her istekte token ekle
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor - token süresi dolmuşsa logout yap
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth API calls
export const authAPI = {
  login: async (credentials) => {
    const response = await api.post('/auth/login', credentials);
    return response.data;
  },
  
  getCurrentUser: async () => {
    const response = await api.get('/auth/me');
    return response.data;
  },
  
  validateToken: async () => {
    const response = await api.get('/auth/validate');
    return response.data;
  },
  
  setupAdmin: async (adminData) => {
    const response = await api.post('/auth/setup-admin', null, {
      params: adminData
    });
    return response.data;
  }
};

// Admin API calls
export const adminAPI = {
  getDashboardStats: async () => {
    const response = await api.get('/admin/dashboard');
    return response.data;
  },
  
  getAllTenants: async (page = 0, size = 10, sortBy = 'createdAt', sortDir = 'desc') => {
    const response = await api.get('/admin/tenants', {
      params: { page, size, sortBy, sortDir }
    });
    return response.data;
  },
  
  getActiveTenants: async () => {
    const response = await api.get('/admin/tenants/active');
    return response.data;
  },
  
  getTenantById: async (tenantId) => {
    const response = await api.get(`/admin/tenants/${tenantId}`);
    return response.data;
  },
  
  createTenant: async (tenantData) => {
    const response = await api.post('/admin/tenants', tenantData);
    return response.data;
  },
  
  toggleTenantStatus: async (tenantId) => {
    const response = await api.patch(`/admin/tenants/${tenantId}/toggle-status`);
    return response.data;
  },
  
  deleteTenant: async (tenantId) => {
    const response = await api.delete(`/admin/tenants/${tenantId}`);
    return response.data;
  },
  
  getTenantUsers: async (tenantId) => {
    const response = await api.get(`/admin/tenants/${tenantId}/users`);
    return response.data;
  },
  
  getTenantStats: async (tenantId) => {
    const response = await api.get(`/admin/tenants/${tenantId}/stats`);
    return response.data;
  }
};

// Utility functions
export const isAuthenticated = () => {
  const token = localStorage.getItem('token');
  return !!token;
};

export const getStoredUser = () => {
  const user = localStorage.getItem('user');
  return user ? JSON.parse(user) : null;
};

export const clearAuth = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
};

export default api;
