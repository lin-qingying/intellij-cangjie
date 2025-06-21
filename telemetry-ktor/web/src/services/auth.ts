import { message } from 'antd';
import api from './api';
import { API, STORAGE_KEYS, JWT } from '../constants';

export interface UserData {
  username: string;
  role?: string;
  displayName?: string;
  email?: string;
  [key: string]: any;
}

export interface AuthResponse {
  success: boolean;
  message?: string;
  token?: string;
  refreshToken?: string;
  expiresIn?: number;
  user?: UserData;
}

export interface TokenRefreshResponse {
  success: boolean;
  token?: string;
  refreshToken?: string;
  expiresIn?: number;
  message?: string;
}

// Token management functions
export const getToken = (): string | null => {
  return localStorage.getItem(STORAGE_KEYS.JWT_TOKEN);
};

export const setToken = (token: string): void => {
  localStorage.setItem(STORAGE_KEYS.JWT_TOKEN, token);
};

export const removeToken = (): void => {
  localStorage.removeItem(STORAGE_KEYS.JWT_TOKEN);
};

export const getRefreshToken = (): string | null => {
  return localStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN);
};

export const setRefreshToken = (token: string): void => {
  localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, token);
};

export const removeRefreshToken = (): void => {
  localStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
};

export const isAuthenticated = (): boolean => {
  return !!getToken();
};

export const getUserData = (): UserData | null => {
  const userData = localStorage.getItem(STORAGE_KEYS.USER);
  return userData ? JSON.parse(userData) : null;
};

export const setUserData = (user: UserData): void => {
  localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(user));
};

// Token refresh functionality
let isRefreshing = false;
let refreshSubscribers: Array<(token: string) => void> = [];

export const subscribeToTokenRefresh = (callback: (token: string) => void): void => {
  refreshSubscribers.push(callback);
};

export const onTokenRefreshed = (token: string): void => {
  refreshSubscribers.forEach(callback => callback(token));
  refreshSubscribers = [];
};

export const refreshToken = async (): Promise<string | null> => {
  const refreshToken = getRefreshToken();
  
  if (!refreshToken) {
    return null;
  }
  
  try {
    const response = await api.post<TokenRefreshResponse>(API.AUTH.REFRESH_TOKEN, { refreshToken });
    
    if (response.data && response.data.success && response.data.token) {
      const newToken = response.data.token;
      setToken(newToken);
      
      if (response.data.refreshToken) {
        setRefreshToken(response.data.refreshToken);
      }
      
      onTokenRefreshed(newToken);
      return newToken;
    }
    
    return null;
  } catch (error) {
    console.error('Token refresh error:', error);
    return null;
  }
};

// Auth API functions
export const loginWithJWT = async (username: string, password: string): Promise<boolean> => {
  try {
    const response = await api.post<AuthResponse>(API.AUTH.LOGIN, { username, password });
    
    if (response.data && response.data.success && response.data.token) {
      setToken(response.data.token);
      
      if (response.data.user) {
        setUserData(response.data.user);
      }
      
      localStorage.setItem(STORAGE_KEYS.IS_AUTHENTICATED, 'true');
      
      // Store refresh token if provided
      if (response.data.refreshToken) {
        setRefreshToken(response.data.refreshToken);
      }
      
      return true;
    } else {
      message.error(response.data.message || '登录失败：无效的响应格式');
      return false;
    }
  } catch (error: any) {
    console.error('Login error:', error);
    message.error(error.response?.data?.message || '登录失败，请检查用户名和密码');
    return false;
  }
};

export const logoutWithJWT = async (): Promise<void> => {
  try {
    await api.post(API.AUTH.LOGOUT);
  } catch (error) {
    console.error('Logout error:', error);
  } finally {
    // Always clear local auth data regardless of API response
    removeToken();
    removeRefreshToken();
    localStorage.removeItem(STORAGE_KEYS.USER);
    localStorage.removeItem(STORAGE_KEYS.IS_AUTHENTICATED);
  }
}; 