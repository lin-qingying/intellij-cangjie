// API related constants
export const API = {
  BASE_URL: '/api',
  AUTH: {
    LOGIN: '/v1/auth/login',
    LOGOUT: '/v1/auth/logout',
    REFRESH_TOKEN: '/v1/auth/refresh',
    CURRENT_USER: '/v1/auth/me',
  },
  USERS: {
    LIST: '/v1/users',
    DETAIL: (username: string) => `/v1/users/${username}`,
    CREATE: '/v1/users',
    UPDATE: (username: string) => `/v1/users/${username}`,
    DELETE: (username: string) => `/v1/users/${username}`,
  },
  EVENTS: {
    LIST: '/v1/events',
    DETAIL: (id: string) => `/v1/events/${id}`,
    DELETE: (id: string) => `/v1/events/${id}`,
    BULK_DELETE: '/v1/events/bulk-delete',
  },
  METADATA: {
    LIST: '/v1/metadata',
    DETAIL: (id: string) => `/v1/metadata/${id}`,
  },
  DASHBOARD: {
    DATA: '/v1/dashboard',
  },
  REPORTS: {
    DATA: '/v1/reports',
  },
  TELEMETRY: {
    STATUS: '/telemetry',
    SUBMIT: '/telemetry',
    EVENTS: '/telemetry/events',
    METADATA: '/telemetry/metadata',
  }
};

// Storage keys
export const STORAGE_KEYS = {
  JWT_TOKEN: 'jwt_token',
  REFRESH_TOKEN: 'refresh_token',
  USER: 'user',
  IS_AUTHENTICATED: 'isAuthenticated'
};

// JWT related settings
export const JWT = {
  TOKEN_EXPIRY_BUFFER: 5 * 60 * 1000, // 5 minutes in milliseconds
};

// Default pagination settings
export const PAGINATION = {
  DEFAULT_PAGE_SIZE: 10,
  DEFAULT_PAGE: 1,
};

// Chart colors
export const CHART_COLORS = [
  '#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1',
  '#13c2c2', '#eb2f96', '#fa8c16', '#a0d911', '#fadb14',
]; 