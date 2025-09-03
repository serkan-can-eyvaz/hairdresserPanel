import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';

// Components
import Navbar from './components/Navbar';
import Sidebar from './components/Sidebar';

// Pages
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Tenants from './pages/Tenants';
import CreateTenant from './pages/CreateTenant';
import Services from './pages/Services';
import Reports from './pages/Reports';
import Subscriptions from './pages/Subscriptions';
import Settings from './pages/Settings';
import Appointments from './pages/Appointments';
import Customers from './pages/Customers';

// Protected Route component
const ProtectedRoute = ({ children }) => {
  const { isLoggedIn, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="spinner mx-auto mb-4"></div>
          <p className="text-gray-600">Yükleniyor...</p>
        </div>
      </div>
    );
  }

  return isLoggedIn ? children : <Navigate to="/login" replace />;
};

// Layout component for authenticated pages
const AppLayout = ({ children }) => {
  return (
    <div className="min-h-screen bg-gray-50">
      <div className="flex">
        <Sidebar />
        <div className="flex-1 flex flex-col">
          <Navbar />
          <main className="flex-1 p-6">
            {children}
          </main>
        </div>
      </div>
    </div>
  );
};

// Main App component
const AppContent = () => {
  const { isLoggedIn } = useAuth();

  return (
    <Router>
      <Routes>
        {/* Login route */}
        <Route 
          path="/login" 
          element={
            isLoggedIn ? <Navigate to="/dashboard" replace /> : <Login />
          } 
        />
        
        {/* Protected routes */}
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <AppLayout>
                <Dashboard />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        
        {/* Tenant routes */}
        <Route
          path="/tenants"
          element={
            <ProtectedRoute>
              <AppLayout>
                <Tenants />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        
        <Route
          path="/tenants/create"
          element={
            <ProtectedRoute>
              <AppLayout>
                <CreateTenant />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        
        <Route
          path="/services"
          element={
            <ProtectedRoute>
              <AppLayout>
                <Services />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        
        <Route
          path="/reports"
          element={
            <ProtectedRoute>
              <AppLayout>
                <Reports />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        
        <Route
          path="/subscriptions"
          element={
            <ProtectedRoute>
              <AppLayout>
                <Subscriptions />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        
        <Route
          path="/settings"
          element={
            <ProtectedRoute>
              <AppLayout>
                <Settings />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        
        <Route
          path="/appointments"
          element={
            <ProtectedRoute>
              <AppLayout>
                <Appointments />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        
        <Route
          path="/customers"
          element={
            <ProtectedRoute>
              <AppLayout>
                <Customers />
              </AppLayout>
            </ProtectedRoute>
          }
        />
        
        {/* Default redirect */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        
        {/* 404 route */}
        <Route 
          path="*" 
          element={
            <div className="min-h-screen flex items-center justify-center">
              <div className="text-center">
                <h1 className="text-4xl font-bold text-gray-900 mb-4">404</h1>
                <p className="text-gray-600 mb-4">Sayfa bulunamadı</p>
                <button
                  onClick={() => window.history.back()}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                >
                  Geri Dön
                </button>
              </div>
            </div>
          } 
        />
      </Routes>
    </Router>
  );
};

// Root App component with AuthProvider
const App = () => {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
};

export default App;