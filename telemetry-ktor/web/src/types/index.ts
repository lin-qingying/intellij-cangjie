// API响应类型
export interface ApiResponse<T = any> {
  success: boolean;
  data: T | null;
  message: string;
}

// 事件类型
export interface TelemetryEvent {
  _id: string;
  id: string;
  category: string;
  name: string;
  value: string;
  timestamp: string;
  properties: Record<string, string>;
  metadataId: string | null;
}

// 元数据类型
export interface TelemetryMetadata {
  _id: string;
  systemId: string;
  pluginVersion: string;
  ideVersion: string;
  ideBuild: string;
  os: string;
  osVersion: string;
  javaVersion: string;
  timestamp: number;
  receivedTimestamp: string;
  formattedTime?: string;
}

// 事件列表响应数据
export interface EventsData {
  events: TelemetryEvent[];
  currentPage: number;
  totalPages: number;
  pageSize: number;
  totalCount: number;
  categories: string[];
  eventNames: string[];
  metadataMap: Record<string, Record<string, string>>;
  formattedTimestamps: Record<string, string>;
}

// 元数据列表响应数据
export interface MetadataData {
  metadata: Record<string, any>[];
  currentPage: number;
  totalPages: number;
  pageSize: number;
  totalCount: number;
}

// 仪表盘数据
export interface DashboardData {
  totalEvents: number;
  totalMetadata: number;
  recentMetadata: Record<string, any>[];
}

// 报表数据
export interface ReportData {
  eventsByCategory: Record<string, number>;
  eventsByTime: Record<string, number>;
  totalEvents: number;
  uniqueUsers: number;
  dailyAverage: number;
  topCategory: string;
  categories: string[];
  timeRange: number;
  groupBy: string;
} 