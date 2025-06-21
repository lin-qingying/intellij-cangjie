import React, { useState, useEffect } from 'react';
import { 
  Card, Typography, Spin, Form, Select, Button, Space, 
  Row, Col, Statistic, Divider, Alert, DatePicker, Segmented, Tabs, Badge,
  Table, Tag, Tooltip as AntTooltip
} from 'antd';
import { 
  BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid, 
  Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell, AreaChart, Area,
  RadarChart, Radar, PolarGrid, PolarAngleAxis, PolarRadiusAxis,
  ScatterChart, Scatter, ZAxis
} from 'recharts';
import { 
  ReloadOutlined, SearchOutlined, CalendarOutlined, 
  BarChartOutlined, RiseOutlined, TeamOutlined, FireOutlined,
  AppstoreOutlined, TableOutlined, InfoCircleOutlined
} from '@ant-design/icons';
import { getReportData } from '../services/api';
import type { ApiResponse, ReportData } from '../types';
import { CHART_COLORS } from '../constants';

const { Title, Text } = Typography;
const { Option } = Select;
const { TabPane } = Tabs;
const { RangePicker } = DatePicker;

interface ReportParams {
  timeRange: number;
  category?: string;
  groupBy: string;
}

// 定义全局样式
const styles = {
  reportsContainer: {
    padding: '0 8px'
  },
  pageHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '24px',
    padding: '16px 0',
    borderBottom: '1px solid #f0f0f0'
  },
  filterCard: {
    marginBottom: '24px',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)',
    borderRadius: '8px'
  },
  loadingContainer: {
    display: 'flex',
    flexDirection: 'column' as const,
    alignItems: 'center',
    justifyContent: 'center',
    padding: '80px 0'
  },
  errorContainer: {
    display: 'flex',
    flexDirection: 'column' as const,
    alignItems: 'center',
    justifyContent: 'center',
    padding: '80px 0'
  },
  reportSummary: {
    marginBottom: '24px'
  },
  summaryHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '16px'
  },
  statCard: {
    height: '100%',
    borderRadius: '8px',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)',
    transition: 'all 0.3s'
  },
  statCardHover: {
    transform: 'translateY(-4px)',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.12)'
  },
  statTrend: {
    marginTop: '8px',
    display: 'flex',
    alignItems: 'center',
    gap: '4px'
  },
  chartCard: {
    borderRadius: '8px',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)',
    height: '100%'
  },
  reportTabs: {
    marginTop: '24px'
  },
  tableCard: {
    borderRadius: '8px',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)'
  },
  infoIcon: {
    marginLeft: '8px',
    color: '#1677ff'
  }
};

// 表格列定义
const tableColumns = [
  {
    title: '时间',
    dataIndex: 'name',
    key: 'name',
    sorter: (a: any, b: any) => a.name.localeCompare(b.name),
  },
  {
    title: '事件数',
    dataIndex: 'count',
    key: 'count',
    sorter: (a: any, b: any) => a.count - b.count,
    render: (count: number) => (
      <Tag color={count > 10 ? 'green' : count > 5 ? 'blue' : 'orange'}>
        {count}
      </Tag>
    ),
  },
  {
    title: '占比',
    dataIndex: 'percentage',
    key: 'percentage',
    render: (percentage: number) => `${percentage.toFixed(2)}%`,
    sorter: (a: any, b: any) => a.percentage - b.percentage,
  }
];

// 生成热力图数据
const generateHeatmapData = (data: { name: string; count: number }[]) => {
  // 将数据按周几和小时分组
  const heatmapData = [];
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  
  // 模拟数据 - 实际项目中应该使用真实数据
  for (let day = 0; day < 7; day++) {
    for (let hour = 0; hour < 24; hour++) {
      // 生成随机值，或者从实际数据中提取
      const value = Math.floor(Math.random() * 100);
      heatmapData.push({
        day: days[day],
        hour: `${hour}:00`,
        value,
        dayIndex: day
      });
    }
  }
  
  return heatmapData;
};

// 生成雷达图数据
const generateRadarData = (categoryData: { name: string; value: number }[]) => {
  // 取前5个类别
  return categoryData.slice(0, 5).map(item => ({
    subject: item.name,
    A: item.value,
    fullMark: Math.max(...categoryData.map(d => d.value)) * 1.2
  }));
};

// 生成堆叠柱状图数据
const generateStackedBarData = (timeData: { name: string; count: number }[]) => {
  // 模拟不同类别的数据
  return timeData.map(item => {
    // 随机生成各类别的占比
    const total = item.count;
    const typeA = Math.floor(total * Math.random() * 0.6);
    const typeB = Math.floor((total - typeA) * Math.random() * 0.7);
    const typeC = total - typeA - typeB;
    
    return {
      name: item.name,
      '类别A': typeA,
      '类别B': typeB,
      '类别C': typeC,
      total
    };
  });
};

const Reports: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState<boolean>(true);
  const [reportData, setReportData] = useState<ReportData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [searchParams, setSearchParams] = useState<ReportParams>({
    timeRange: 7,
    groupBy: 'day'
  });
  const [viewMode, setViewMode] = useState<string>('charts');
  const [hoveredCard, setHoveredCard] = useState<number | null>(null);
  const [activeTabKey, setActiveTabKey] = useState<string>('overview');

  // 获取报表数据
  const fetchReportData = async (params: ReportParams) => {
    try {
      setLoading(true);
      const response = await getReportData(params);
      const result = response.data as ApiResponse<ReportData>;
      
      if (result.success && result.data) {
        setReportData(result.data);
        setError(null);
      } else {
        setError(result.message || '获取报表数据失败');
      }
    } catch (err: any) {
      setError(err.message || '获取报表数据失败');
    } finally {
      setLoading(false);
    }
  };

  // 初始加载
  useEffect(() => {
    fetchReportData(searchParams);
  }, []);

  // 处理搜索
  const handleSearch = (values: any) => {
    const params: ReportParams = {
      timeRange: values.timeRange || 7,
      category: values.category,
      groupBy: values.groupBy || 'day'
    };
    setSearchParams(params);
    fetchReportData(params);
  };

  // 重置搜索
  const resetSearch = () => {
    form.resetFields();
    const params: ReportParams = {
      timeRange: 7,
      groupBy: 'day'
    };
    setSearchParams(params);
    fetchReportData(params);
  };

  // 转换类别数据为图表格式
  const getCategoryChartData = () => {
    if (!reportData || !reportData.eventsByCategory) return [];
    
    return Object.entries(reportData.eventsByCategory).map(([category, count]) => ({
      name: category,
      value: count
    }));
  };

  // 转换时间数据为图表格式
  const getTimeChartData = () => {
    if (!reportData || !reportData.eventsByTime) return [];
    
    return Object.entries(reportData.eventsByTime).map(([time, count]) => ({
      name: time,
      count: count
    }));
  };

  // 获取表格数据（添加占比）
  const getTableData = () => {
    const timeData = getTimeChartData();
    const totalEvents = reportData?.totalEvents || 0;
    
    return timeData.map(item => ({
      ...item,
      percentage: totalEvents > 0 ? (item.count / totalEvents) * 100 : 0
    }));
  };

  // 获取趋势变化百分比
  const getTrendPercentage = () => {
    if (!reportData || !reportData.trendPercentage) return 0;
    return reportData.trendPercentage;
  };

  // 获取时间范围显示文本
  const getTimeRangeText = () => {
    switch (searchParams.timeRange) {
      case 1: return '最近24小时';
      case 7: return '最近7天';
      case 30: return '最近30天';
      case 90: return '最近3个月';
      default: return '最近7天';
    }
  };

  return (
    <div style={styles.reportsContainer}>
      <div style={styles.pageHeader}>
        <div className="header-content">
          <Title level={2} style={{ margin: 0 }}>
            <CalendarOutlined style={{ marginRight: 12 }} />
            统计报表
          </Title>
          <Text type="secondary">
            查看和分析遥测事件的统计数据和趋势
          </Text>
        </div>
        <div className="view-toggle">
          <Segmented
            value={viewMode}
            onChange={value => setViewMode(value as string)}
            options={[
              {
                label: '图表视图',
                value: 'charts',
                icon: <AppstoreOutlined />
              },
              {
                label: '表格视图',
                value: 'table',
                icon: <TableOutlined />
              }
            ]}
          />
        </div>
      </div>
      
      <Card style={styles.filterCard}>
        <Form
          form={form}
          layout="horizontal"
          onFinish={handleSearch}
          initialValues={{
            timeRange: 7,
            category: undefined,
            groupBy: 'day'
          }}
        >
          <Row gutter={24}>
            <Col xs={24} sm={24} md={8} lg={8}>
              <Form.Item name="timeRange" label="时间范围">
                <Select placeholder="选择时间范围">
                  <Option value={1}>最近24小时</Option>
                  <Option value={7}>最近7天</Option>
                  <Option value={30}>最近30天</Option>
                  <Option value={90}>最近3个月</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col xs={24} sm={24} md={8} lg={8}>
              <Form.Item name="category" label="事件类别">
                <Select
                  allowClear
                  placeholder="选择事件类别"
                  showSearch
                  optionFilterProp="children"
                >
                  {reportData?.categories?.map(category => (
                    <Option key={category} value={category}>{category}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col xs={24} sm={24} md={8} lg={8}>
              <Form.Item name="groupBy" label="分组方式">
                <Select placeholder="选择分组方式">
                  <Option value="hour">按小时</Option>
                  <Option value="day">按天</Option>
                  <Option value="week">按周</Option>
                  <Option value="month">按月</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>
          
          <Row>
            <Col span={24} style={{ textAlign: 'right' }}>
              <Space>
                <Button type="primary" icon={<SearchOutlined />} htmlType="submit">
                  应用筛选
                </Button>
                <Button icon={<ReloadOutlined />} onClick={resetSearch}>
                  重置
                </Button>
              </Space>
            </Col>
          </Row>
        </Form>
      </Card>

      {loading ? (
        <div style={styles.loadingContainer}>
          <Spin size="large" />
          <Text style={{ marginTop: 16 }}>加载报表数据中...</Text>
        </div>
      ) : error ? (
        <div style={styles.errorContainer}>
          <Alert message="错误" description={error} type="error" showIcon />
          <Button 
            type="primary" 
            style={{ marginTop: 16 }} 
            onClick={() => fetchReportData(searchParams)}
          >
            重试
          </Button>
        </div>
      ) : (
        <>
          <div style={styles.reportSummary}>
            <div style={styles.summaryHeader}>
              <Title level={4}>数据概览</Title>
              <Badge 
                status="processing" 
                text={`数据时段: ${getTimeRangeText()}`} 
              />
            </div>
            
            <Row gutter={[16, 16]}>
              <Col xs={24} sm={12} md={12} lg={6}>
                <Card 
                  style={{
                    ...styles.statCard,
                    ...(hoveredCard === 0 ? styles.statCardHover : {})
                  }}
                  onMouseEnter={() => setHoveredCard(0)}
                  onMouseLeave={() => setHoveredCard(null)}
                >
                  <Statistic
                    title="事件总数"
                    value={reportData?.totalEvents || 0}
                    valueStyle={{ color: '#3f8600' }}
                    prefix={<BarChartOutlined />}
                  />
                  <div style={styles.statTrend}>
                    <RiseOutlined style={{ color: getTrendPercentage() >= 0 ? '#3f8600' : '#cf1322' }} />
                    <Text type={getTrendPercentage() >= 0 ? 'success' : 'danger'}>
                      {`${Math.abs(getTrendPercentage())}% ${getTrendPercentage() >= 0 ? '增长' : '下降'}`}
                    </Text>
                  </div>
                </Card>
              </Col>
              <Col xs={24} sm={12} md={12} lg={6}>
                <Card 
                  style={{
                    ...styles.statCard,
                    ...(hoveredCard === 1 ? styles.statCardHover : {})
                  }}
                  onMouseEnter={() => setHoveredCard(1)}
                  onMouseLeave={() => setHoveredCard(null)}
                >
                  <Statistic
                    title="唯一用户数"
                    value={reportData?.uniqueUsers || 0}
                    valueStyle={{ color: '#1677ff' }}
                    prefix={<TeamOutlined />}
                  />
                </Card>
              </Col>
              <Col xs={24} sm={12} md={12} lg={6}>
                <Card 
                  style={{
                    ...styles.statCard,
                    ...(hoveredCard === 2 ? styles.statCardHover : {})
                  }}
                  onMouseEnter={() => setHoveredCard(2)}
                  onMouseLeave={() => setHoveredCard(null)}
                >
                  <Statistic
                    title="日均事件数"
                    value={reportData?.dailyAverage || 0}
                    precision={2}
                    valueStyle={{ color: '#722ed1' }}
                    prefix={<BarChartOutlined />}
                  />
                </Card>
              </Col>
              <Col xs={24} sm={12} md={12} lg={6}>
                <Card 
                  style={{
                    ...styles.statCard,
                    ...(hoveredCard === 3 ? styles.statCardHover : {})
                  }}
                  onMouseEnter={() => setHoveredCard(3)}
                  onMouseLeave={() => setHoveredCard(null)}
                >
                  <Statistic
                    title="最活跃类别"
                    value={reportData?.topCategory || '-'}
                    valueStyle={{ color: '#fa8c16' }}
                    prefix={<FireOutlined />}
                  />
                </Card>
              </Col>
            </Row>
          </div>

          {viewMode === 'charts' ? (
            <Tabs 
              defaultActiveKey="overview" 
              activeKey={activeTabKey}
              onChange={key => setActiveTabKey(key)}
              style={styles.reportTabs}
            >
              <TabPane tab="总览" key="overview">
                <Row gutter={[16, 16]}>
                  <Col xs={24} md={12}>
                    <Card title="事件类别分布" style={styles.chartCard}>
                      <ResponsiveContainer width="100%" height={320}>
                        <PieChart>
                          <Pie
                            data={getCategoryChartData()}
                            cx="50%"
                            cy="50%"
                            labelLine={true}
                            label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                            outerRadius={100}
                            innerRadius={60}
                            fill="#8884d8"
                            dataKey="value"
                            paddingAngle={2}
                          >
                            {getCategoryChartData().map((entry, index) => (
                              <Cell key={`cell-${index}`} fill={CHART_COLORS[index % CHART_COLORS.length]} />
                            ))}
                          </Pie>
                          <Tooltip formatter={(value) => [`${value} 事件`, '数量']} />
                          <Legend layout="vertical" verticalAlign="middle" align="right" />
                        </PieChart>
                      </ResponsiveContainer>
                    </Card>
                  </Col>
                  <Col xs={24} md={12}>
                    <Card title="事件时间趋势" style={styles.chartCard}>
                      <ResponsiveContainer width="100%" height={320}>
                        <AreaChart
                          data={getTimeChartData()}
                          margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
                        >
                          <defs>
                            <linearGradient id="colorCount" x1="0" y1="0" x2="0" y2="1">
                              <stop offset="5%" stopColor="#8884d8" stopOpacity={0.8}/>
                              <stop offset="95%" stopColor="#8884d8" stopOpacity={0.1}/>
                            </linearGradient>
                          </defs>
                          <CartesianGrid strokeDasharray="3 3" vertical={false} />
                          <XAxis dataKey="name" />
                          <YAxis />
                          <Tooltip />
                          <Legend />
                          <Area 
                            type="monotone" 
                            dataKey="count" 
                            stroke="#8884d8" 
                            fillOpacity={1} 
                            fill="url(#colorCount)" 
                            name="事件数" 
                          />
                        </AreaChart>
                      </ResponsiveContainer>
                    </Card>
                  </Col>
                </Row>
              </TabPane>
              <TabPane tab="详细分析" key="details">
                <Row gutter={[16, 16]}>
                  <Col xs={24}>
                    <Card title="事件类别时间分布" style={styles.chartCard}>
                      <ResponsiveContainer width="100%" height={400}>
                        <BarChart
                          data={getTimeChartData()}
                          margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
                        >
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="name" />
                          <YAxis />
                          <Tooltip />
                          <Legend />
                          <Bar dataKey="count" fill="#8884d8" name="事件数" radius={[4, 4, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                    </Card>
                  </Col>
                </Row>
              </TabPane>
              <TabPane tab="高级图表" key="advanced">
                <Row gutter={[16, 16]}>
                  <Col xs={24} md={12}>
                    <Card 
                      title={
                        <span>
                          类别雷达图
                          <AntTooltip title="展示前5个事件类别的数据分布">
                            <InfoCircleOutlined style={styles.infoIcon} />
                          </AntTooltip>
                        </span>
                      } 
                      style={styles.chartCard}
                    >
                      <ResponsiveContainer width="100%" height={400}>
                        <RadarChart outerRadius={150} data={generateRadarData(getCategoryChartData())}>
                          <PolarGrid />
                          <PolarAngleAxis dataKey="subject" />
                          <PolarRadiusAxis />
                          <Radar
                            name="事件数量"
                            dataKey="A"
                            stroke="#8884d8"
                            fill="#8884d8"
                            fillOpacity={0.6}
                          />
                          <Tooltip />
                          <Legend />
                        </RadarChart>
                      </ResponsiveContainer>
                    </Card>
                  </Col>
                  <Col xs={24} md={12}>
                    <Card 
                      title={
                        <span>
                          堆叠柱状图
                          <AntTooltip title="按不同类别展示事件分布">
                            <InfoCircleOutlined style={styles.infoIcon} />
                          </AntTooltip>
                        </span>
                      } 
                      style={styles.chartCard}
                    >
                      <ResponsiveContainer width="100%" height={400}>
                        <BarChart
                          data={generateStackedBarData(getTimeChartData())}
                          margin={{ top: 20, right: 30, left: 20, bottom: 5 }}
                        >
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis dataKey="name" />
                          <YAxis />
                          <Tooltip />
                          <Legend />
                          <Bar dataKey="类别A" stackId="a" fill="#8884d8" />
                          <Bar dataKey="类别B" stackId="a" fill="#82ca9d" />
                          <Bar dataKey="类别C" stackId="a" fill="#ffc658" />
                        </BarChart>
                      </ResponsiveContainer>
                    </Card>
                  </Col>
                </Row>
                <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                  <Col xs={24}>
                    <Card 
                      title={
                        <span>
                          事件热力图
                          <AntTooltip title="展示一周内不同时段的事件分布">
                            <InfoCircleOutlined style={styles.infoIcon} />
                          </AntTooltip>
                        </span>
                      } 
                      style={styles.chartCard}
                    >
                      <ResponsiveContainer width="100%" height={400}>
                        <ScatterChart
                          margin={{ top: 20, right: 20, bottom: 20, left: 20 }}
                        >
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis 
                            type="category" 
                            dataKey="hour" 
                            name="时间" 
                            allowDuplicatedCategory={false} 
                          />
                          <YAxis 
                            type="category" 
                            dataKey="day" 
                            name="星期" 
                            allowDuplicatedCategory={false}
                            width={60}
                          />
                          <ZAxis 
                            type="number"
                            dataKey="value"
                            range={[50, 500]}
                            name="数量"
                          />
                          <Tooltip 
                            cursor={{ strokeDasharray: '3 3' }}
                            formatter={(value) => [`${value} 事件`, '数量']}
                          />
                          <Scatter 
                            data={generateHeatmapData(getTimeChartData())} 
                            fill="#8884d8"
                            shape="circle"
                          />
                        </ScatterChart>
                      </ResponsiveContainer>
                    </Card>
                  </Col>
                </Row>
              </TabPane>
            </Tabs>
          ) : (
            <Card title="事件数据表格" style={styles.tableCard}>
              <div style={{ marginBottom: 16 }}>
                <Text type="secondary">
                  显示 {getTimeRangeText()} 的事件数据，共 {reportData?.totalEvents || 0} 条记录
                </Text>
              </div>
              <Table 
                columns={tableColumns} 
                dataSource={getTableData()}
                rowKey="name"
                pagination={{ 
                  pageSize: 10, 
                  showSizeChanger: true, 
                  pageSizeOptions: ['10', '20', '50'],
                  showTotal: (total) => `共 ${total} 条记录`
                }}
              />
            </Card>
          )}
        </>
      )}
    </div>
  );
};

export default Reports; 