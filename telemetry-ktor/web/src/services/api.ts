import axios from 'axios';

const BASE_URL = '/api';

const api = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for API calls
api.interceptors.request.use(
  (config) => {
    const isAuthenticated = localStorage.getItem('isAuthenticated') === 'true';
    
    if (isAuthenticated) {
      // Add session cookie header if needed
      config.withCredentials = true;
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for API calls
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    // Handle 401 Unauthorized errors
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('isAuthenticated');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth APIs
export const login = (username: string, password: string) => {
  return api.post('/v1/auth/login', { username, password });
};

export const logout = () => {
  return api.post('/v1/auth/logout');
};

export const getCurrentUser = () => {
  return api.get('/v1/auth/me');
};

// User Management APIs
export const getUsers = (params: { page?: number; pageSize?: number; }) => {
  return api.get('/v1/users', { params });
};

export const getUser = (username: string) => {
  return api.get(`/v1/users/${username}`);
};

export const createUser = (userData: any) => {
  return api.post('/v1/users', userData);
};

export const updateUser = (username: string, userData: any) => {
  return api.put(`/v1/users/${username}`, userData);
};

export const deleteUser = (username: string) => {
  return api.delete(`/v1/users/${username}`);
};

// Dashboard APIs
export const getDashboardData = () => {
  return api.get('/v1/dashboard');
};

// Events APIs
export const getEvents = (params: { 
  page?: number; 
  pageSize?: number; 
  category?: string; 
  name?: string;
  startDate?: string;
  endDate?: string;
  groupBy?: string;
}) => {
  return api.get('/v1/events', { params });
};

export const deleteEvent = (id: string) => {
  return api.delete(`/v1/events/${id}`);
};

export const bulkDeleteEvents = (ids: string[]) => {
  return api.post('/v1/events/bulk-delete', { ids });
};

// Metadata APIs
export const getMetadata = (params: { 
  page?: number; 
  pageSize?: number;
}) => {
  return api.get('/v1/metadata', { params });
};

// Reports APIs
export const getReportData = (params: {
  timeRange?: number;
  category?: string;
  groupBy?: string;
}) => {
  return api.get('/v1/reports', { params });
};

// Telemetry Direct APIs
export const getTelemetryStatus = () => {
  return api.get('/telemetry');
};

export const submitTelemetry = (data: any) => {
  return api.post('/telemetry', data);
};

export const getTelemetryEvents = (limit?: number) => {
  return api.get('/telemetry/events', { params: { limit } });
};

export const getTelemetryMetadata = (limit?: number) => {
  return api.get('/telemetry/metadata', { params: { limit } });
};

export default api; 