import React, { useState, useEffect } from 'react';
import {
  Card, Table, Typography, Button, Space, Input, Select, DatePicker,
  Tag, Modal, Tabs, Tooltip, message, Spin, Badge, Row, Col, Form, Divider,
  Dropdown, Menu, Pagination
} from 'antd';
import {
  FilterOutlined, SearchOutlined, ReloadOutlined, InfoCircleOutlined,
  DownOutlined, ExportOutlined, QuestionCircleOutlined,
  ArrowRightOutlined, EyeOutlined, FileTextOutlined, CloseCircleOutlined
} from '@ant-design/icons';
import { getEvents } from '../services/api';
import type { ApiResponse, EventsData, TelemetryEvent } from '../types';
import dayjs from 'dayjs';
import { Link } from 'react-router-dom';

const { Title, Text, Paragraph } = Typography;
const { Option } = Select;
const { RangePicker } = DatePicker;
const { TabPane } = Tabs;

const Events: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(true);
  const [eventsData, setEventsData] = useState<EventsData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [propertiesModalVisible, setPropertiesModalVisible] = useState<boolean>(false);
  const [currentEvent, setCurrentEvent] = useState<TelemetryEvent | null>(null);
  const [searchText, setSearchText] = useState<string>('');
  const [filterVisible, setFilterVisible] = useState<boolean>(true);
  const [helpModalVisible, setHelpModalVisible] = useState<boolean>(false);
  
  // 筛选条件
  const [filters, setFilters] = useState({
    category: '',
    name: '',
    startDate: '',
    endDate: '',
    page: 1,
    pageSize: 10,
    metadataId: ''
  });

  // 获取事件列表
  const fetchEvents = async (params = filters) => {
    try {
      setLoading(true);
      const response = await getEvents(params);
      const result = response.data as ApiResponse<EventsData>;
      
      if (result.success && result.data) {
        setEventsData(result.data);
        setError(null);
      } else {
        setError(result.message || '获取事件列表失败');
      }
    } catch (err: any) {
      setError(err.message || '获取事件列表失败');
    } finally {
      setLoading(false);
    }
  };

  // 初始加载
  useEffect(() => {
    fetchEvents();
  }, []);

  // 处理筛选条件变更
  const handleFilterChange = (changedValues: any) => {
    const newFilters = { ...filters, ...changedValues, page: 1 };
    setFilters(newFilters);
    fetchEvents(newFilters);
  };

  // 处理分页变更
  const handlePageChange = (page: number, pageSize?: number) => {
    const newFilters = { ...filters, page, pageSize: pageSize || filters.pageSize };
    setFilters(newFilters);
    fetchEvents(newFilters);
  };

  // 处理查看属性
  const showProperties = (event: TelemetryEvent) => {
    setCurrentEvent(event);
    setPropertiesModalVisible(true);
  };

  // 重置筛选条件
  const resetFilters = () => {
    const defaultFilters = {
      category: '',
      name: '',
      startDate: '',
      endDate: '',
      page: 1,
      pageSize: 10,
      metadataId: ''
    };
    setFilters(defaultFilters);
    fetchEvents(defaultFilters);
  };

  // 导出数据菜单
  const exportMenu = (
    <Menu items={[
      {
        key: '1',
        label: '导出为CSV',
        icon: <FileTextOutlined />,
        onClick: () => message.info('CSV导出功能正在开发中')
      },
      {
        key: '2',
        label: '导出为JSON',
        icon: <FileTextOutlined />,
        onClick: () => message.info('JSON导出功能正在开发中')
      }
    ]} />
  );

  // 表格列配置
  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 100,
      ellipsis: true,
    },
    {
      title: '类别',
      dataIndex: 'category',
      key: 'category',
      width: 100,
      render: (category: string) => {
        let color = 'blue';
        if (category === 'error') color = 'red';
        else if (category === 'warning') color = 'orange';
        else if (category === 'info') color = 'cyan';
        else if (category === 'success') color = 'green';
        
        return <Tag color={color}>{category}</Tag>;
      },
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      width: 150,
    },
    {
      title: '值',
      dataIndex: 'value',
      key: 'value',
      width: 200,
      ellipsis: true,
      render: (value: string) => (
        <Tooltip title={value}>
          <span>{value}</span>
        </Tooltip>
      ),
    },
    {
      title: '时间',
      key: 'timestamp',
      width: 150,
      render: (record: TelemetryEvent) => {
        const formattedTime = eventsData?.formattedTimestamps?.[record.id];
        return formattedTime || record.timestamp;
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_: any, record: TelemetryEvent) => (
        <Space>
          <Button
            type="primary"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => showProperties(record)}
          >
            快速查看
          </Button>
          <Link to={`/events/${record.id}`}>
            <Button
              type="default"
              size="small"
              icon={<ArrowRightOutlined />}
            >
              详情
            </Button>
          </Link>
        </Space>
      ),
    },
  ];

  // 获取类别标签颜色
  const getCategoryColor = (category: string) => {
    switch (category.toLowerCase()) {
      case 'error':
        return 'red';
      case 'warning':
        return 'orange';
      case 'info':
        return 'cyan';
      case 'success':
        return 'green';
      default:
        return 'blue';
    }
  };

  return (
    <div>
      <Title level={2}>事件列表</Title>
      
      {/* 筛选卡片 */}
      <Card 
        title={
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <FilterOutlined style={{ marginRight: 8 }} />
            <span>筛选条件</span>
          </div>
        }
        extra={
          <Button 
            type="text" 
            icon={filterVisible ? <DownOutlined /> : <ArrowRightOutlined />} 
            onClick={() => setFilterVisible(!filterVisible)}
          />
        }
        style={{ marginBottom: 16 }}
      >
        {filterVisible && (
          <Form layout="vertical" onFinish={() => {}}>
            <Row gutter={16}>
              <Col xs={24} md={24} lg={24}>
                <Input
                  placeholder="搜索事件..."
                  prefix={<SearchOutlined />}
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                  style={{ marginBottom: 16 }}
                  suffix={
                    searchText ? (
                      <CloseCircleOutlined onClick={() => setSearchText('')} />
                    ) : null
                  }
                />
                <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
                  搜索将在当前页面结果中进行筛选
                </Text>
              </Col>
              
              <Col xs={24} md={8}>
                <Form.Item label="事件类别">
                  <Select
                    placeholder="选择事件类别"
                    value={filters.category}
                    onChange={(value) => handleFilterChange({ category: value })}
                    allowClear
                    style={{ width: '100%' }}
                  >
                    {eventsData?.categories?.map((category) => (
                      <Option key={category} value={category}>
                        {category}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              
              <Col xs={24} md={8}>
                <Form.Item label="事件名称">
                  <Select
                    placeholder="选择事件名称"
                    value={filters.name}
                    onChange={(value) => handleFilterChange({ name: value })}
                    allowClear
                    style={{ width: '100%' }}
                  >
                    {eventsData?.eventNames?.map((name) => (
                      <Option key={name} value={name}>
                        {name}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              
              <Col xs={24} md={8}>
                <Form.Item label="日期范围">
                  <RangePicker
                    style={{ width: '100%' }}
                    onChange={(dates) => {
                      if (dates) {
                        handleFilterChange({
                          startDate: dates[0]?.format('YYYY-MM-DD'),
                          endDate: dates[1]?.format('YYYY-MM-DD')
                        });
                      } else {
                        handleFilterChange({
                          startDate: '',
                          endDate: ''
                        });
                      }
                    }}
                  />
                </Form.Item>
              </Col>
              
              <Col xs={24} md={8}>
                <Form.Item label="每页显示">
                  <Select
                    value={filters.pageSize}
                    onChange={(value) => handleFilterChange({ pageSize: value })}
                    style={{ width: '100%' }}
                  >
                    <Option value={10}>10 条/页</Option>
                    <Option value={25}>25 条/页</Option>
                    <Option value={50}>50 条/页</Option>
                    <Option value={100}>100 条/页</Option>
                  </Select>
                </Form.Item>
              </Col>
              
              <Col xs={24} md={16}>
                <div style={{ display: 'flex', alignItems: 'flex-end', height: '100%', marginBottom: 24 }}>
                  <Space>
                    <Button
                      type="primary"
                      icon={<FilterOutlined />}
                      onClick={() => fetchEvents(filters)}
                    >
                      应用筛选
                    </Button>
                    <Button
                      icon={<ReloadOutlined />}
                      onClick={resetFilters}
                    >
                      重置筛选
                    </Button>
                  </Space>
                  
                  {eventsData?.totalCount !== undefined && (
                    <Text style={{ marginLeft: 'auto' }}>
                      共 <Text strong>{eventsData.totalCount}</Text> 条记录
                    </Text>
                  )}
                </div>
              </Col>
            </Row>
          </Form>
        )}
      </Card>
      
      {/* 事件列表卡片 */}
      <Card
        title={
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <InfoCircleOutlined style={{ marginRight: 8 }} />
            <span>事件列表</span>
            {filters.metadataId && (
              <Tag color="blue" style={{ marginLeft: 8 }}>
                按元数据ID过滤: {filters.metadataId}
                <CloseCircleOutlined
                  onClick={() => handleFilterChange({ metadataId: '' })}
                  style={{ marginLeft: 4, cursor: 'pointer' }}
                />
              </Tag>
            )}
          </div>
        }
        extra={
          <Space>
            <Dropdown overlay={exportMenu}>
              <Button icon={<ExportOutlined />}>
                导出 <DownOutlined />
              </Button>
            </Dropdown>
            <Button
              icon={<QuestionCircleOutlined />}
              onClick={() => setHelpModalVisible(true)}
            />
          </Space>
        }
      >
        {loading ? (
          <div style={{ textAlign: 'center', padding: '50px' }}>
            <Spin size="large" />
          </div>
        ) : error ? (
          <div style={{ textAlign: 'center', color: 'red', padding: '50px' }}>
            {error}
          </div>
        ) : (
          <>
            <Table
              rowKey="id"
              columns={columns}
              dataSource={eventsData?.events.filter(event => {
                if (!searchText) return true;
                const searchLower = searchText.toLowerCase();
                return (
                  event.id.toLowerCase().includes(searchLower) ||
                  event.category.toLowerCase().includes(searchLower) ||
                  event.name.toLowerCase().includes(searchLower) ||
                  event.value.toLowerCase().includes(searchLower)
                );
              }) || []}
              pagination={false}
              scroll={{ x: 'max-content' }}
            />
            
            {eventsData?.totalPages && eventsData.totalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16 }}>
                <Pagination
                  current={filters.page}
                  pageSize={filters.pageSize}
                  total={eventsData.totalCount}
                  showQuickJumper
                  showSizeChanger={false}
                  onChange={handlePageChange}
                  showTotal={(total) => `共 ${total} 条记录`}
                />
              </div>
            )}
          </>
        )}
      </Card>
      
      {/* 属性模态框 */}
      <Modal
        title={
          <div>
            <InfoCircleOutlined style={{ marginRight: 8, color: '#1890ff' }} />
            事件属性
          </div>
        }
        open={propertiesModalVisible}
        onCancel={() => setPropertiesModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setPropertiesModalVisible(false)}>
            关闭
          </Button>
        ]}
        width={600}
      >
        {currentEvent && (
          <>
            <div style={{ marginBottom: 16 }}>
              <Text type="secondary">事件ID: {currentEvent.id}</Text>
              <Title level={5} style={{ margin: '8px 0' }}>{currentEvent.name}</Title>
              <Tag color={getCategoryColor(currentEvent.category)}>{currentEvent.category}</Tag>
            </div>
            
            {currentEvent.metadataId && eventsData?.metadataMap && eventsData.metadataMap[currentEvent.metadataId] && (
              <>
                <Divider orientation="left">元数据信息</Divider>
                <Row gutter={[16, 8]}>
                  {Object.entries(eventsData.metadataMap[currentEvent.metadataId]).map(([key, value]) => (
                    <Col span={12} key={key}>
                      <Text type="secondary" style={{ display: 'block', fontSize: 12 }}>{key}</Text>
                      <div>{value || '-'}</div>
                    </Col>
                  ))}
                </Row>
              </>
            )}
            
            <Divider orientation="left">事件属性</Divider>
            {Object.keys(currentEvent.properties).length > 0 ? (
              <Table
                dataSource={Object.entries(currentEvent.properties).map(([key, value]) => ({
                  key,
                  value
                }))}
                columns={[
                  {
                    title: '键',
                    dataIndex: 'key',
                    key: 'key',
                    width: '40%',
                    render: (text) => <Text code>{text}</Text>
                  },
                  {
                    title: '值',
                    dataIndex: 'value',
                    key: 'value',
                    width: '60%',
                    render: (value) => {
                      // 如果是字符串且可能包含HTML代码，则直接渲染HTML
                      if (typeof value === 'string') {
                        if (value.includes('<div') || value.includes('<span')) {
                          return <div dangerouslySetInnerHTML={{ __html: value }} />;
                        }
                        
                        // 尝试解析JSON
                        try {
                          if ((value.startsWith('{') && value.endsWith('}')) || 
                              (value.startsWith('[') && value.endsWith(']'))) {
                            const parsed = JSON.parse(value);
                            return (
                              <div style={{ maxHeight: '200px', overflow: 'auto' }}>
                                <pre style={{ 
                                  whiteSpace: 'pre-wrap', 
                                  wordBreak: 'break-word',
                                  backgroundColor: '#f5f5f5',
                                  padding: '8px',
                                  borderRadius: '4px',
                                  fontSize: '12px',
                                  margin: 0
                                }}>
                                  {JSON.stringify(parsed, null, 2)}
                                </pre>
                              </div>
                            );
                          }
                        } catch (e) {
                          // 解析失败，按普通文本处理
                        }
                      }
                      
                      // 默认显示
                      return <span>{String(value)}</span>;
                    }
                  }
                ]}
                pagination={false}
                size="small"
              />
            ) : (
              <div style={{ textAlign: 'center', padding: '20px 0' }}>
                <Text type="secondary">没有属性数据</Text>
              </div>
            )}
          </>
        )}
      </Modal>
      
      {/* 帮助模态框 */}
      <Modal
        title={
          <div>
            <QuestionCircleOutlined style={{ marginRight: 8, color: '#1890ff' }} />
            帮助信息
          </div>
        }
        open={helpModalVisible}
        onCancel={() => setHelpModalVisible(false)}
        footer={[
          <Button key="ok" type="primary" onClick={() => setHelpModalVisible(false)}>
            了解了
          </Button>
        ]}
      >
        <Title level={5}>如何使用筛选功能</Title>
        <ul>
          <li>使用<Text strong>事件类别</Text>筛选特定类型的事件</li>
          <li>使用<Text strong>事件名称</Text>筛选特定名称的事件</li>
          <li>使用<Text strong>日期范围</Text>筛选特定时间段的事件</li>
          <li>使用<Text strong>搜索框</Text>在当前页面结果中快速查找</li>
        </ul>
        
        <Title level={5}>事件类别说明</Title>
        <Space>
          <Tag color="blue">默认</Tag>
          <Tag color="green">success</Tag>
          <Tag color="cyan">info</Tag>
          <Tag color="orange">warning</Tag>
          <Tag color="red">error</Tag>
        </Space>
        
        <Divider />
        
        <Title level={5}>导出数据</Title>
        <Paragraph>
          您可以将当前筛选结果导出为CSV或JSON格式，方便进行进一步分析。
        </Paragraph>
      </Modal>
    </div>
  );
};

export default Events; 