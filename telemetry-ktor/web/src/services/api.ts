import axios from 'axios';
import type { AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import { API, STORAGE_KEYS } from '../constants';
import { refreshToken, subscribeToTokenRefresh } from './auth';

const api = axios.create({
  baseURL: API.BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for API calls
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(STORAGE_KEYS.JWT_TOKEN);
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

let isRefreshing = false;
let failedQueue: { resolve: (value: unknown) => void; reject: (reason?: any) => void }[] = [];

const processQueue = (error: AxiosError | null, token: string | null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  
  failedQueue = [];
};

// Response interceptor for API calls
api.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean };
    
    // If the error is not 401 or this request already tried to refresh, reject
    if (!error.response || error.response.status !== 401 || originalRequest._retry) {
      if (error.response && error.response.status === 401) {
        // Clear auth data for definitive 401s
        localStorage.removeItem(STORAGE_KEYS.JWT_TOKEN);
        localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
        localStorage.removeItem(STORAGE_KEYS.IS_AUTHENTICATED);
        localStorage.removeItem(STORAGE_KEYS.USER);
        window.location.href = '/login';
      }
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      })
        .then(token => {
          if (token && originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${token}`;
          }
          return axios(originalRequest);
        })
        .catch(err => Promise.reject(err));
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      const newToken = await refreshToken();
      
      if (newToken) {
        // Update the authorization header
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
        }
        // Process any requests that were waiting for the token
        processQueue(null, newToken);
        // Continue with the original request
        return axios(originalRequest);
      } else {
        // Clear auth data if refresh failed
        localStorage.removeItem(STORAGE_KEYS.JWT_TOKEN);
        localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
        localStorage.removeItem(STORAGE_KEYS.IS_AUTHENTICATED);
        localStorage.removeItem(STORAGE_KEYS.USER);
        processQueue(error, null);
        window.location.href = '/login';
        return Promise.reject(error);
      }
    } catch (refreshError) {
      processQueue(refreshError as AxiosError, null);
      // Clear auth data
      localStorage.removeItem(STORAGE_KEYS.JWT_TOKEN);
      localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
      localStorage.removeItem(STORAGE_KEYS.IS_AUTHENTICATED);
      localStorage.removeItem(STORAGE_KEYS.USER);
      window.location.href = '/login';
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

// Auth APIs
export const login = (username: string, password: string) => {
  return api.post(API.AUTH.LOGIN, { username, password });
};

export const logout = () => {
  return api.post(API.AUTH.LOGOUT);
};

export const getCurrentUser = () => {
  return api.get(API.AUTH.CURRENT_USER);
};

// User Management APIs
export const getUsers = (params: { page?: number; pageSize?: number; }) => {
  return api.get(API.USERS.LIST, { params });
};

export const getUser = (username: string) => {
  return api.get(API.USERS.DETAIL(username));
};

export const createUser = (userData: any) => {
  return api.post(API.USERS.CREATE, userData);
};

export const updateUser = (username: string, userData: any) => {
  return api.put(API.USERS.UPDATE(username), userData);
};

export const deleteUser = (username: string) => {
  return api.delete(API.USERS.DELETE(username));
};

// Dashboard APIs
export const getDashboardData = () => {
  return api.get(API.DASHBOARD.DATA);
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
  metadataId?: string;
}) => {
  return api.get(API.EVENTS.LIST, { params });
};

export const deleteEvent = (id: string) => {
  return api.delete(API.EVENTS.DELETE(id));
};

export const bulkDeleteEvents = (ids: string[]) => {
  return api.post(API.EVENTS.BULK_DELETE, { ids });
};

// Metadata APIs
export const getMetadata = (params: { 
  page?: number; 
  pageSize?: number;
}) => {
  return api.get(API.METADATA.LIST, { params });
};

export const getMetadataById = (id: string) => {
  return api.get(API.METADATA.DETAIL(id));
};

// Reports APIs
export const getReportData = (params: {
  timeRange?: number;
  category?: string;
  groupBy?: string;
}) => {
  return api.get(API.REPORTS.DATA, { params });
};

// Analytics APIs
export const getAnalyticsData = (params: {
  timeRange?: number;
  category?: string;
  analysisType?: 'trend' | 'comparison' | 'forecast' | 'correlation';
}) => {
  return api.get(API.ANALYTICS.DATA, { params });
};

// Telemetry Direct APIs
export const getTelemetryStatus = () => {
  return api.get(API.TELEMETRY.STATUS);
};

export const submitTelemetry = (data: any) => {
  return api.post(API.TELEMETRY.SUBMIT, data);
};

export const getTelemetryEvents = (limit?: number) => {
  return api.get(API.TELEMETRY.EVENTS, { params: { limit } });
};

export const getTelemetryMetadata = (limit?: number) => {
  return api.get(API.TELEMETRY.METADATA, { params: { limit } });
};

export default api; 