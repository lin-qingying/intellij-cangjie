import React, { useState, useEffect, useCallback } from 'react';
import { 
  Row, Col, Card, Typography, Spin, Select, DatePicker, 
  Button, Space, Tabs, Empty, Alert, Statistic, Divider,
  Radio, Tooltip, Table, Tag, message
} from 'antd';
import { 
  BarChartOutlined, LineChartOutlined, PieChartOutlined,
  AreaChartOutlined, InfoCircleOutlined, ReloadOutlined,
  FilterOutlined, DownloadOutlined, RiseOutlined, FallOutlined,
  CalendarOutlined, SearchOutlined
} from '@ant-design/icons';
import { getAnalyticsData } from '../services/api';
import type { AnalyticsData, AnalysisType } from '../types';
import { CHART_COLORS } from '../constants';
import dayjs from 'dayjs';
import {
  Line, Bar, Pie, Area, DualAxes
} from '@ant-design/plots';

const { Title, Text, Paragraph } = Typography;
const { RangePicker } = DatePicker;
const { TabPane } = Tabs;
const { Option } = Select;

const Analytics: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(true);
  const [analyticsData, setAnalyticsData] = useState<AnalyticsData | null>(null);
  const [error, setError] = useState<string | null>(null);
  
  // 过滤条件
  const [timeRange, setTimeRange] = useState<number>(30); // 默认30天
  const [category, setCategory] = useState<string | null>(null);
  const [analysisType, setAnalysisType] = useState<AnalysisType>('trend');
  
  // 图表配置
  const [chartType, setChartType] = useState<'line' | 'bar' | 'area'>('line');
  
  // 获取分析数据
  const fetchAnalyticsData = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getAnalyticsData({
        timeRange,
        category,
        analysisType,
      });
      
      const result = response.data;
      
      if (result.success && result.data) {
        setAnalyticsData(result.data);
        setError(null);
      } else {
        setError(result.message || '获取分析数据失败');
      }
    } catch (err: any) {
      setError(err.message || '获取分析数据失败');
      message.error('获取分析数据失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  }, [timeRange, category, analysisType]);
  
  useEffect(() => {
    fetchAnalyticsData();
  }, [fetchAnalyticsData]);
  
  // 处理时间范围变化
  const handleTimeRangeChange = (value: number) => {
    setTimeRange(value);
  };
  
  // 处理类别变化
  const handleCategoryChange = (value: string | null) => {
    setCategory(value);
  };
  
  // 处理分析类型变化
  const handleAnalysisTypeChange = (e: any) => {
    setAnalysisType(e.target.value);
  };
  
  // 处理图表类型变化
  const handleChartTypeChange = (value: 'line' | 'bar' | 'area') => {
    setChartType(value);
  };
  
  // 重新加载数据
  const handleRefresh = () => {
    fetchAnalyticsData();
    message.success('数据已刷新');
  };
  
  // 渲染时间序列图表
  const renderTimeSeriesChart = () => {
    if (!analyticsData) return <Empty description="暂无数据" />;
    
    const { dailyData } = analyticsData;
    const data = Object.entries(dailyData).map(([date, count]) => ({
      date,
      count,
    }));
    
    const commonConfig = {
      data,
      xField: 'date',
      yField: 'count',
      smooth: true,
      animation: true,
      xAxis: {
        type: 'time',
        tickCount: 5,
        label: {
          formatter: (text: string) => {
            return dayjs(text).format('MM-DD');
          },
        },
      },
      tooltip: {
        formatter: (datum: any) => {
          return {
            name: '事件数',
            value: datum.count,
            title: dayjs(datum.date).format('YYYY-MM-DD'),
          };
        },
      },
      legend: {
        position: 'top-right',
      },
      color: CHART_COLORS[0],
    };
    
    if (chartType === 'line') {
      return (
        <Line 
          {...commonConfig}
          point={{
            size: 4,
            shape: 'circle',
          }}
        />
      );
    } else if (chartType === 'bar') {
      return (
        <Bar 
          {...commonConfig}
          label={{
            position: 'top',
            style: {
              fill: '#aaa',
              fontSize: 12,
            },
            formatter: (datum: any) => {
              return datum.count > 10 ? datum.count : '';
            },
          }}
        />
      );
    } else {
      return (
        <Area 
          {...commonConfig}
          areaStyle={{
            fill: `l(270) 0:${CHART_COLORS[0]}00 1:${CHART_COLORS[0]}ff`,
          }}
        />
      );
    }
  };
  
  // 渲染类别分布图表
  const renderCategoryDistributionChart = () => {
    if (!analyticsData) return <Empty description="暂无数据" />;
    
    const { eventsByCategory } = analyticsData;
    const data = Object.entries(eventsByCategory).map(([category, count]) => ({
      category,
      count,
    }));
    
    return (
      <Pie
        data={data}
        angleField="count"
        colorField="category"
        radius={0.8}
        innerRadius={0.5}
        label={{
          type: 'outer',
          content: '{name}: {percentage}',
        }}
        interactions={[
          {
            type: 'element-active',
          },
        ]}
        legend={{
          position: 'bottom',
          layout: 'horizontal',
        }}
        tooltip={{
          formatter: (datum) => {
            return {
              name: datum.category,
              value: `${datum.count} (${((datum.count / analyticsData.totalEvents) * 100).toFixed(2)}%)`,
            };
          },
        }}
        color={CHART_COLORS}
      />
    );
  };
  
  // 渲染比较分析
  const renderComparisonAnalysis = () => {
    if (!analyticsData || !analyticsData.comparisonData) {
      return <Empty description="暂无比较数据" />;
    }
    
    const { comparisonData } = analyticsData;
    const { currentPeriod, previousPeriod, change } = comparisonData;
    
    return (
      <Row gutter={[16, 16]}>
        <Col span={8}>
          <Card>
            <Statistic
              title="事件数变化"
              value={change.eventsPercentage}
              precision={2}
              valueStyle={{ color: change.eventsPercentage >= 0 ? '#3f8600' : '#cf1322' }}
              prefix={change.eventsPercentage >= 0 ? <RiseOutlined /> : <FallOutlined />}
              suffix="%"
            />
            <div style={{ marginTop: 10 }}>
              <Text type="secondary">
                当前: {currentPeriod.events} | 上期: {previousPeriod.events}
              </Text>
            </div>
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="用户数变化"
              value={
                ((currentPeriod.users - previousPeriod.users) / previousPeriod.users) * 100
              }
              precision={2}
              valueStyle={{ 
                color: currentPeriod.users >= previousPeriod.users ? '#3f8600' : '#cf1322' 
              }}
              prefix={currentPeriod.users >= previousPeriod.users ? <RiseOutlined /> : <FallOutlined />}
              suffix="%"
            />
            <div style={{ marginTop: 10 }}>
              <Text type="secondary">
                当前: {currentPeriod.users} | 上期: {previousPeriod.users}
              </Text>
            </div>
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="人均事件数"
              value={currentPeriod.users ? (currentPeriod.events / currentPeriod.users).toFixed(2) : 0}
              precision={2}
              valueStyle={{ color: '#1890ff' }}
            />
            <div style={{ marginTop: 10 }}>
              <Text type="secondary">
                上期: {previousPeriod.users ? (previousPeriod.events / previousPeriod.users).toFixed(2) : 0}
              </Text>
            </div>
          </Card>
        </Col>
      </Row>
    );
  };
  
  // 渲染预测分析
  const renderForecastAnalysis = () => {
    if (!analyticsData || !analyticsData.forecastData) {
      return <Empty description="暂无预测数据" />;
    }
    
    const { dailyData, forecastData } = analyticsData;
    
    // 合并历史数据和预测数据
    const historicalData = Object.entries(dailyData).map(([date, count]) => ({
      date,
      count,
      type: '历史数据',
    }));
    
    const forecastedData = Object.entries(forecastData).map(([date, count]) => ({
      date,
      count,
      type: '预测数据',
    }));
    
    const data = [...historicalData, ...forecastedData];
    
    return (
      <Line
        data={data}
        xField="date"
        yField="count"
        seriesField="type"
        smooth={true}
        animation={true}
        xAxis={{
          type: 'time',
          tickCount: 5,
          label: {
            formatter: (text: string) => {
              return dayjs(text).format('MM-DD');
            },
          },
        }}
        tooltip={{
          formatter: (datum: any) => {
            return {
              name: datum.type,
              value: datum.count,
              title: dayjs(datum.date).format('YYYY-MM-DD'),
            };
          },
        }}
        legend={{
          position: 'top-right',
        }}
        color={['#1890ff', '#faad14']}
        lineStyle={({ type }) => {
          if (type === '预测数据') {
            return {
              lineDash: [4, 4],
              opacity: 0.8,
            };
          }
          return {
            opacity: 0.8,
          };
        }}
        point={{
          size: 4,
          shape: 'circle',
          style: ({ type }) => {
            if (type === '预测数据') {
              return {
                fill: '#faad14',
                stroke: '#faad14',
              };
            }
            return {
              fill: '#1890ff',
              stroke: '#1890ff',
            };
          },
        }}
      />
    );
  };
  
  // 渲染相关性分析
  const renderCorrelationAnalysis = () => {
    if (!analyticsData || !analyticsData.correlationData) {
      return <Empty description="暂无相关性数据" />;
    }
    
    const { correlationData } = analyticsData;
    const data = Object.entries(correlationData).map(([factor, value]) => ({
      factor,
      value: parseFloat(value.toFixed(2)),
    })).sort((a, b) => Math.abs(b.value) - Math.abs(a.value));
    
    const columns = [
      {
        title: '因素',
        dataIndex: 'factor',
        key: 'factor',
      },
      {
        title: '相关系数',
        dataIndex: 'value',
        key: 'value',
        render: (value: number) => {
          const color = value > 0.7 ? 'green' : 
                       value > 0.4 ? 'blue' : 
                       value > 0 ? 'cyan' :
                       value > -0.4 ? 'orange' : 'red';
                       
          return (
            <Tag color={color}>
              {value}
            </Tag>
          );
        },
      },
      {
        title: '强度',
        dataIndex: 'strength',
        key: 'strength',
        render: (_: any, record: { value: number }) => {
          const value = Math.abs(record.value);
          let strength;
          let color;
          
          if (value > 0.7) {
            strength = '强';
            color = 'green';
          } else if (value > 0.4) {
            strength = '中';
            color = 'blue';
          } else {
            strength = '弱';
            color = 'gray';
          }
          
          return <Tag color={color}>{strength}</Tag>;
        },
      },
      {
        title: '方向',
        dataIndex: 'direction',
        key: 'direction',
        render: (_: any, record: { value: number }) => {
          if (record.value > 0) {
            return <Tag color="green">正相关</Tag>;
          } else if (record.value < 0) {
            return <Tag color="red">负相关</Tag>;
          }
          return <Tag>无相关</Tag>;
        },
      },
    ];
    
    return (
      <Table 
        dataSource={data}
        columns={columns}
        rowKey="factor"
        pagination={false}
        size="middle"
      />
    );
  };
  
  // 渲染分析内容
  const renderAnalysisContent = () => {
    switch (analysisType) {
      case 'trend':
        return (
          <>
            <Card 
              title="时间趋势分析"
              extra={
                <Radio.Group value={chartType} onChange={(e) => handleChartTypeChange(e.target.value)}>
                  <Radio.Button value="line"><LineChartOutlined /></Radio.Button>
                  <Radio.Button value="bar"><BarChartOutlined /></Radio.Button>
                  <Radio.Button value="area"><AreaChartOutlined /></Radio.Button>
                </Radio.Group>
              }
              style={{ marginBottom: 16 }}
            >
              <div style={{ height: 400 }}>
                {renderTimeSeriesChart()}
              </div>
            </Card>
            <Row gutter={[16, 16]}>
              <Col span={12}>
                <Card title="类别分布">
                  <div style={{ height: 300 }}>
                    {renderCategoryDistributionChart()}
                  </div>
                </Card>
              </Col>
              <Col span={12}>
                <Card title="关键指标">
                  <Row gutter={[16, 16]}>
                    <Col span={8}>
                      <Statistic 
                        title="总事件数"
                        value={analyticsData?.totalEvents || 0}
                      />
                    </Col>
                    <Col span={8}>
                      <Statistic 
                        title="独立用户数"
                        value={analyticsData?.uniqueUsers || 0}
                      />
                    </Col>
                    <Col span={8}>
                      <Statistic 
                        title="日均事件"
                        value={analyticsData?.dailyAverage || 0}
                        precision={2}
                      />
                    </Col>
                    <Col span={8}>
                      <Statistic 
                        title="增长率"
                        value={analyticsData?.growthRate || 0}
                        precision={2}
                        suffix="%"
                        valueStyle={{ 
                          color: (analyticsData?.growthRate || 0) >= 0 ? '#3f8600' : '#cf1322' 
                        }}
                        prefix={(analyticsData?.growthRate || 0) >= 0 ? <RiseOutlined /> : <FallOutlined />}
                      />
                    </Col>
                    <Col span={8}>
                      <Statistic 
                        title="活跃度"
                        value={analyticsData?.activityScore || 0}
                        precision={2}
                        suffix="/100"
                      />
                    </Col>
                    <Col span={8}>
                      <Statistic 
                        title="参与度"
                        value={analyticsData?.engagementRate || 0}
                        precision={2}
                        suffix="%"
                      />
                    </Col>
                  </Row>
                </Card>
              </Col>
            </Row>
          </>
        );
      case 'comparison':
        return (
          <>
            <Card title="比较分析" style={{ marginBottom: 16 }}>
              {renderComparisonAnalysis()}
            </Card>
            <Card title="时间段对比">
              <div style={{ height: 400 }}>
                {renderTimeSeriesChart()}
              </div>
            </Card>
          </>
        );
      case 'forecast':
        return (
          <Card title="预测分析">
            <Alert 
              message="预测说明" 
              description="基于历史数据的时间序列分析，预测未来7天的事件趋势。虚线部分为预测数据，仅供参考。"
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
            />
            <div style={{ height: 400 }}>
              {renderForecastAnalysis()}
            </div>
          </Card>
        );
      case 'correlation':
        return (
          <Card title="相关性分析">
            <Alert 
              message="相关性说明" 
              description="分析各因素与事件数量的相关性，相关系数范围为[-1,1]，绝对值越大表示相关性越强。"
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
            />
            {renderCorrelationAnalysis()}
          </Card>
        );
      default:
        return <Empty description="请选择分析类型" />;
    }
  };
  
  return (
    <div className="analytics-page">
      <div className="page-header" style={{ marginBottom: 16 }}>
        <Row justify="space-between" align="middle">
          <Col>
            <Title level={2}>
              <BarChartOutlined /> 数据分析
            </Title>
          </Col>
          <Col>
            <Space>
              <Button 
                type="primary" 
                icon={<ReloadOutlined />} 
                onClick={handleRefresh}
                loading={loading}
              >
                刷新
              </Button>
              <Button 
                icon={<DownloadOutlined />} 
                disabled={!analyticsData}
              >
                导出数据
              </Button>
            </Space>
          </Col>
        </Row>
        <Paragraph type="secondary">
          深入分析用户行为数据，发现趋势和模式，优化产品体验。
        </Paragraph>
      </div>
      
      <Card className="filter-card" style={{ marginBottom: 16 }}>
        <Row gutter={16} align="middle">
          <Col>
            <Text strong><FilterOutlined /> 筛选：</Text>
          </Col>
          <Col>
            <Select
              placeholder="时间范围"
              style={{ width: 120 }}
              value={timeRange}
              onChange={handleTimeRangeChange}
            >
              <Option value={7}>近7天</Option>
              <Option value={30}>近30天</Option>
              <Option value={90}>近90天</Option>
              <Option value={180}>近半年</Option>
              <Option value={365}>近一年</Option>
            </Select>
          </Col>
          <Col>
            <Select
              placeholder="选择类别"
              style={{ width: 150 }}
              value={category}
              onChange={handleCategoryChange}
              allowClear
            >
              {analyticsData?.categories?.map(cat => (
                <Option key={cat} value={cat}>{cat}</Option>
              ))}
            </Select>
          </Col>
          <Col flex="auto">
            <Divider type="vertical" style={{ margin: '0 16px' }} />
            <Text strong>分析类型：</Text>
            <Radio.Group value={analysisType} onChange={handleAnalysisTypeChange}>
              <Radio.Button value="trend">趋势分析</Radio.Button>
              <Radio.Button value="comparison">比较分析</Radio.Button>
              <Radio.Button value="forecast">预测分析</Radio.Button>
              <Radio.Button value="correlation">相关性分析</Radio.Button>
            </Radio.Group>
          </Col>
        </Row>
      </Card>
      
      {error && (
        <Alert
          message="错误"
          description={error}
          type="error"
          showIcon
          closable
          style={{ marginBottom: 16 }}
        />
      )}
      
      {loading ? (
        <div style={{ textAlign: 'center', padding: '50px 0' }}>
          <Spin size="large" tip="加载数据中..." />
        </div>
      ) : (
        renderAnalysisContent()
      )}
    </div>
  );
};

export default Analytics;
