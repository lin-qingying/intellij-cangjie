import React, { useState, useEffect } from 'react';
import { 
  Card, Typography, Spin, Form, Select, Button, Space, 
  Row, Col, Statistic, Divider, Alert
} from 'antd';
import { 
  BarChart, Bar, LineChart, Line, XAxis, YAxis, CartesianGrid, 
  Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell
} from 'recharts';
import { ReloadOutlined, SearchOutlined } from '@ant-design/icons';
import { getReportData } from '../services/api';
import type { ApiResponse, ReportData } from '../types';
import { CHART_COLORS } from '../constants';

const { Title } = Typography;
const { Option } = Select;

interface ReportParams {
  timeRange: number;
  category?: string;
  groupBy: string;
}

const Reports: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState<boolean>(true);
  const [reportData, setReportData] = useState<ReportData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [searchParams, setSearchParams] = useState<ReportParams>({
    timeRange: 7,
    groupBy: 'day'
  });

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

  return (
    <div>
      <Title level={2}>统计报表</Title>
      
      <Card className="filter-form">
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
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="timeRange" label="时间范围">
                <Select placeholder="选择时间范围">
                  <Option value={1}>最近24小时</Option>
                  <Option value={7}>最近7天</Option>
                  <Option value={30}>最近30天</Option>
                  <Option value={90}>最近3个月</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="category" label="事件类别">
                <Select
                  allowClear
                  placeholder="选择事件类别"
                >
                  {reportData?.categories?.map(category => (
                    <Option key={category} value={category}>{category}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={8}>
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
                  应用
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
        <div style={{ textAlign: 'center', padding: '50px' }}>
          <Spin size="large" />
        </div>
      ) : error ? (
        <div style={{ textAlign: 'center', padding: '50px' }}>
          <Alert message="错误" description={error} type="error" showIcon />
        </div>
      ) : (
        <>
          <Row gutter={16} style={{ marginTop: 16 }}>
            <Col span={6}>
              <Card>
                <Statistic
                  title="事件总数"
                  value={reportData?.totalEvents || 0}
                  valueStyle={{ color: '#3f8600' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="唯一用户数"
                  value={reportData?.uniqueUsers || 0}
                  valueStyle={{ color: '#1677ff' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="日均事件数"
                  value={reportData?.dailyAverage || 0}
                  precision={2}
                  valueStyle={{ color: '#722ed1' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="最活跃类别"
                  value={reportData?.topCategory || '-'}
                  valueStyle={{ color: '#fa8c16' }}
                />
              </Card>
            </Col>
          </Row>

          <Row gutter={16} style={{ marginTop: 16 }}>
            <Col span={12}>
              <Card title="事件类别分布" className="chart-container">
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie
                      data={getCategoryChartData()}
                      cx="50%"
                      cy="50%"
                      labelLine={true}
                      label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                      outerRadius={80}
                      fill="#8884d8"
                      dataKey="value"
                    >
                      {getCategoryChartData().map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={CHART_COLORS[index % CHART_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(value) => [`${value} 事件`, '数量']} />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              </Card>
            </Col>
            <Col span={12}>
              <Card title="事件时间趋势" className="chart-container">
                <ResponsiveContainer width="100%" height={300}>
                  <LineChart
                    data={getTimeChartData()}
                    margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Line type="monotone" dataKey="count" stroke="#8884d8" activeDot={{ r: 8 }} name="事件数" />
                  </LineChart>
                </ResponsiveContainer>
              </Card>
            </Col>
          </Row>

          <Row style={{ marginTop: 16 }}>
            <Col span={24}>
              <Card title="事件类别时间分布" className="chart-container">
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
                    <Bar dataKey="count" fill="#8884d8" name="事件数" />
                  </BarChart>
                </ResponsiveContainer>
              </Card>
            </Col>
          </Row>
        </>
      )}
    </div>
  );
};

export default Reports; 