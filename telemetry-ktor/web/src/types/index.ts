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
  metadata: TelemetryMetadata[];
  currentPage: number;
  totalPages: number;
  pageSize: number;
  totalCount: number;
  formattedTimestamps?: Record<string, string>;
}

// 仪表盘数据
export interface DashboardData {
  totalEvents: number;
  totalMetadata: number;
  recentMetadata: Record<string, any>[];
  uniqueUsers: number;
  systemStatus: {
    cpu: number;
    memory: number;
    disk: number;
    status: 'normal' | 'warning' | 'error';
  };
  todayNewEvents: number;
  todayNewMetadata: number;
  userGrowth: string;
  lastSyncTime: string;
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
  trendPercentage: number;
}

// 数据分析类型
export type AnalysisType = 'trend' | 'comparison' | 'forecast' | 'correlation';

// 比较数据
export interface ComparisonData {
  currentPeriod: {
    events: number;
    users: number;
  };
  previousPeriod: {
    events: number;
    users: number;
  };
  change: {
    events: number;
    eventsPercentage: number;
  };
}

// 数据分析响应数据
export interface AnalyticsData {
  // 基础信息
  timeRange: number;
  category: string | null;
  analysisType: AnalysisType;
  
  // 关键指标
  totalEvents: number;
  uniqueUsers: number;
  dailyAverage: number;
  growthRate: number;
  activityScore: number;
  engagementRate: number;
  topCategory: string;
  
  // 类别数据
  categories: string[];
  eventsByCategory: Record<string, number>;
  
  // 时间序列数据
  dailyData: Record<string, number>;
  weeklyData: Record<string, number>;
  monthlyData: Record<string, number>;
  
  // 分析类型特定数据
  comparisonData?: ComparisonData;
  forecastData?: Record<string, number>;
  correlationData?: Record<string, number>;
} 