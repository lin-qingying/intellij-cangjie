import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { ConfigProvider, Spin } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { useEffect, useState } from 'react';
import AppLayout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Events from './pages/Events';
import EventDetails from './pages/EventDetails';
import Metadata from './pages/Metadata';
import MetadataDetails from './pages/MetadataDetails';
import Reports from './pages/Reports';
import Users from './pages/Users';
import Settings from './pages/Settings';
import Login from './pages/Login';
import PrivacyPolicy from './pages/PrivacyPolicy';
import { isAuthenticated } from './services/auth';
import './styles/index.css';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

const ProtectedRoute = ({ children }: ProtectedRouteProps) => {
  const location = useLocation();
  const [checking, setChecking] = useState(true);
  const [auth, setAuth] = useState(false);
  
  useEffect(() => {
    // 检查认证状态
    const checkAuth = () => {
      const authenticated = isAuthenticated();
      setAuth(authenticated);
      setChecking(false);
    };
    
    checkAuth();
  }, []);
  
  if (checking) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh' 
      }}>
        <Spin size="large" tip="验证登录状态..." />
      </div>
    );
  }
  
  if (!auth) {
    // 保存当前位置，以便登录后重定向回来
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  
  return <>{children}</>;
};

const App = () => {
  const [authState, setAuthState] = useState(false);
  
  useEffect(() => {
    const checkAuth = () => {
      setAuthState(isAuthenticated());
    };
    
    checkAuth();
    window.addEventListener('storage', checkAuth);
    
    return () => {
      window.removeEventListener('storage', checkAuth);
    };
  }, []);

  return (
    <ConfigProvider locale={zhCN}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/privacy-policy" element={<PrivacyPolicy />} />
        
        <Route path="/" element={
          <ProtectedRoute>
            <AppLayout>
              <Dashboard />
            </AppLayout>
          </ProtectedRoute>
        } />
        
        <Route path="/dashboard" element={
          <ProtectedRoute>
            <AppLayout>
              <Dashboard />
            </AppLayout>
          </ProtectedRoute>
        } />
        
        <Route path="/events" element={
          <ProtectedRoute>
            <AppLayout>
              <Events />
            </AppLayout>
          </ProtectedRoute>
        } />
        
        <Route path="/events/:id" element={
          <ProtectedRoute>
            <AppLayout>
              <EventDetails />
            </AppLayout>
          </ProtectedRoute>
        } />
        
        <Route path="/metadata" element={
          <ProtectedRoute>
            <AppLayout>
              <Metadata />
            </AppLayout>
          </ProtectedRoute>
        } />
        
        <Route path="/metadata/:id" element={
          <ProtectedRoute>
            <AppLayout>
              <MetadataDetails />
            </AppLayout>
          </ProtectedRoute>
        } />
        
        <Route path="/reports" element={
          <ProtectedRoute>
            <AppLayout>
              <Reports />
            </AppLayout>
          </ProtectedRoute>
        } />
        
        <Route path="/users" element={
          <ProtectedRoute>
            <AppLayout>
              <Users />
            </AppLayout>
          </ProtectedRoute>
        } />
        
        <Route path="/settings" element={
          <ProtectedRoute>
            <AppLayout>
              <Settings />
            </AppLayout>
          </ProtectedRoute>
        } />
        
        {/* Redirect to login for any unknown routes */}
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </ConfigProvider>
  );
}

export default App;
