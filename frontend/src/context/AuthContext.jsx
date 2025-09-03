import React, { createContext, useContext, useState, useEffect } from 'react';
import { authAPI, isAuthenticated, getStoredUser, clearAuth } from '../utils/api';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  // Sayfa yüklendiğinde token kontrolü yap
  useEffect(() => {
    const initAuth = async () => {
      try {
        if (isAuthenticated()) {
          const storedUser = getStoredUser();
          if (storedUser) {
            // Token geçerliliğini kontrol et
            await authAPI.validateToken();
            setUser(storedUser);
            setIsLoggedIn(true);
          } else {
            // Stored user yoksa current user'ı al
            const currentUser = await authAPI.getCurrentUser();
            setUser(currentUser);
            setIsLoggedIn(true);
            localStorage.setItem('user', JSON.stringify(currentUser));
          }
        }
      } catch (error) {
        console.error('Auth initialization failed:', error);
        clearAuth();
        setUser(null);
        setIsLoggedIn(false);
      } finally {
        setLoading(false);
      }
    };

    initAuth();
  }, []);

  const login = async (credentials) => {
    try {
      setLoading(true);
      const response = await authAPI.login(credentials);
      
      // Token'ı ve user bilgilerini sakla
      localStorage.setItem('token', response.token);
      localStorage.setItem('user', JSON.stringify({
        username: response.username,
        role: response.role,
        tenantId: response.tenantId,
        tenantName: response.tenantName
      }));
      
      setUser({
        username: response.username,
        role: response.role,
        tenantId: response.tenantId,
        tenantName: response.tenantName
      });
      setIsLoggedIn(true);
      
      return response;
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    clearAuth();
    setUser(null);
    setIsLoggedIn(false);
  };

  const isSuperAdmin = () => {
    return user?.role === 'SUPER_ADMIN';
  };

  const isTenantAdmin = () => {
    return user?.role === 'TENANT_ADMIN';
  };

  const value = {
    user,
    isLoggedIn,
    loading,
    login,
    logout,
    isSuperAdmin,
    isTenantAdmin,
    token: localStorage.getItem('token'), // Token'ı da ekle
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
