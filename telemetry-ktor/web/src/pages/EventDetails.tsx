import React, { useState, useEffect } from 'react';
import {
  Card, Typography, Button, Tag, Descriptions, Table, Space,
  Row, Col, Divider, message, Spin, Modal, Breadcrumb, Alert,
  Tabs, Tooltip, Empty, Skeleton, Statistic
} from 'antd';
import {
  FileTextOutlined, InfoCircleOutlined, DatabaseOutlined,
  CodeOutlined, ArrowLeftOutlined, LinkOutlined,
  ClockCircleOutlined, CheckCircleOutlined, WarningOutlined,
  ExclamationCircleOutlined, CloseCircleOutlined, AppstoreOutlined,
  CalendarOutlined, RightOutlined
} from '@ant-design/icons';
import { useParams, useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import { API } from '../constants';
import type { ApiResponse, TelemetryEvent, TelemetryMetadata } from '../types';
import api from "../services/api.ts";

const { Title, Text, Paragraph } = Typography;
const { TabPane } = Tabs;

// 扩展元数据类型以包含可能的额外属性
interface EnhancedTelemetryMetadata {
  // 基本 TelemetryMetadata 属性
  _id: string;
  systemId: string;
  pluginVersion: string;
  ideVersion: string;
  os: string;
  osVersion: string;
  timestamp: number | string;
  ideBuild?: string;
  
  // 额外可选属性
  projectType?: string;
  additionalData?: Record<string, any>;
  javaVersion?: string;
}

interface EventDetailsParams {
  id: string;
}

const EventDetails: React.FC = () => {
  const { id } = useParams<keyof EventDetailsParams>() as EventDetailsParams;
  const navigate = useNavigate();
  const [loading, setLoading] = useState<boolean>(true);
  const [event, setEvent] = useState<TelemetryEvent | null>(null);
  const [metadata, setMetadata] = useState<EnhancedTelemetryMetadata | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [formattedTime, setFormattedTime] = useState<string>('');
  const [activeTab, setActiveTab] = useState<string>('properties');

  // 获取事件详情
  useEffect(() => {
    const fetchEventDetails = async () => {
      try {
        setLoading(true);
        
        // 获取事件详情
        const response = await api.get(`${API.EVENTS.DETAIL(id)}`);
        const result = response.data as ApiResponse<{
          event: TelemetryEvent;
          metadata?: TelemetryMetadata;
          formattedTime: string;
        }>;
        
        if (result.success && result.data) {
          setEvent(result.data.event);
          setFormattedTime(result.data.formattedTime);
          
          // 如果响应中包含元数据
          if (result.data.metadata) {
            // 使用类型断言将 TelemetryMetadata 转换为 EnhancedTelemetryMetadata
            // 先转为 unknown 再转为目标类型，避免直接转换错误
            const enhancedMetadata = result.data.metadata as unknown as EnhancedTelemetryMetadata;
            setMetadata(enhancedMetadata);
          }
          
          setError(null);
        } else {
          setError(result.message || '获取事件详情失败');
        }
      } catch (err: any) {
        setError(err.message || '获取事件详情失败');
      } finally {
        setLoading(false);
      }
    };
    
    fetchEventDetails();
  }, [id]);

  // 获取类别标签颜色
  const getCategoryColor = (category?: string) => {
    if (!category) return 'blue';
    
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

  // 获取类别图标
  const getCategoryIcon = (category?: string) => {
    if (!category) return <InfoCircleOutlined />;
    
    switch (category.toLowerCase()) {
      case 'error':
        return <CloseCircleOutlined style={{ color: '#ff4d4f' }} />;
      case 'warning':
        return <ExclamationCircleOutlined style={{ color: '#faad14' }} />;
      case 'info':
        return <InfoCircleOutlined style={{ color: '#1890ff' }} />;
      case 'success':
        return <CheckCircleOutlined style={{ color: '#52c41a' }} />;
      default:
        return <AppstoreOutlined style={{ color: '#1890ff' }} />;
    }
  };

  // 渲染页面标题和基本信息
  const renderPageHeader = () => (
    <div className="page-header-wrapper" style={{ marginBottom: 24 }}>
      <Card
        bordered={false}
        style={{
          borderRadius: '12px',
          overflow: 'hidden',
          boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
          background: event ? `linear-gradient(135deg, ${getCategoryColor(event.category) === 'blue' ? '#1890ff 0%, #096dd9' : 
                                                       getCategoryColor(event.category) === 'red' ? '#ff4d4f 0%, #cf1322' :
                                                       getCategoryColor(event.category) === 'orange' ? '#faad14 0%, #d48806' :
                                                       getCategoryColor(event.category) === 'green' ? '#52c41a 0%, #389e0d' :
                                                       getCategoryColor(event.category) === 'cyan' ? '#13c2c2 0%, #08979c' :
                                                       '#1890ff 0%, #096dd9'} 100%)` : 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)',
        }}
        bodyStyle={{ padding: '24px' }}
      >
        <div style={{ position: 'relative' }}>
          <div 
            style={{ 
              position: 'absolute', 
              right: -20, 
              top: -20, 
              width: 200, 
              height: 200, 
              borderRadius: '50%', 
              background: 'rgba(255, 255, 255, 0.1)',
              zIndex: 0
            }} 
          />
          <div 
            style={{ 
              position: 'absolute', 
              left: 30, 
              bottom: -50, 
              width: 100, 
              height: 100, 
              borderRadius: '50%', 
              background: 'rgba(255, 255, 255, 0.1)',
              zIndex: 0
            }} 
          />
          
          <Row gutter={[16, 16]} align="middle" style={{ position: 'relative', zIndex: 1 }}>
            <Col xs={24} md={16}>
              <div style={{ marginBottom: 16 }}>
                <Breadcrumb 
                  items={[
                    { 
                      title: (
                        <Link to="/events" style={{ color: 'rgba(255, 255, 255, 0.85)' }}>
                          事件列表
                        </Link>
                      )
                    },
                    { 
                      title: <span style={{ color: 'white' }}>事件详情</span> 
                    }
                  ]} 
                  separator={<RightOutlined style={{ fontSize: 10, color: 'rgba(255, 255, 255, 0.85)' }} />}
                />
              </div>
              
              <div style={{ color: 'white' }}>
                <div style={{ display: 'flex', alignItems: 'center', marginBottom: 8 }}>
                  {event && getCategoryIcon(event.category)}
                  <Title level={2} style={{ color: 'white', margin: '0 0 0 8px' }}>
                    {event?.name || '事件详情'}
                  </Title>
                </div>
                
                {event && (
                  <Paragraph style={{ color: 'rgba(255, 255, 255, 0.85)', margin: '8px 0 0 0' }}>
                    ID: {event.id}
                  </Paragraph>
                )}
              </div>
            </Col>
            
            <Col xs={24} md={8} style={{ textAlign: 'right' }}>
              <Space>
                <Tag 
                  icon={<ClockCircleOutlined />} 
                  color="default"
                  style={{ 
                    background: 'rgba(255, 255, 255, 0.2)', 
                    color: 'white',
                    border: 'none',
                    padding: '4px 8px',
                    fontSize: '14px'
                  }}
                >
                  {formattedTime || '未知时间'}
                </Tag>
                <Button
                  icon={<ArrowLeftOutlined />}
                  onClick={() => navigate('/events')}
                  style={{ 
                    background: 'rgba(255, 255, 255, 0.2)', 
                    borderColor: 'rgba(255, 255, 255, 0.3)',
                    color: 'white'
                  }}
                >
                  返回列表
                </Button>
              </Space>
            </Col>
          </Row>
          
          {event && (
            <Row gutter={[16, 16]} style={{ marginTop: 24, position: 'relative', zIndex: 1 }}>
              <Col xs={24} sm={12} md={8}>
                <Card style={{ 
                  borderRadius: '8px', 
                  background: 'rgba(255, 255, 255, 0.15)',
                  backdropFilter: 'blur(10px)',
                  border: 'none'
                }}>
                  <Statistic
                    title={<span style={{ color: 'rgba(255, 255, 255, 0.85)' }}>类别</span>}
                    value={event.category}
                    valueStyle={{ color: 'white', fontWeight: 'bold' }}
                    prefix={getCategoryIcon(event.category)}
                  />
                </Card>
              </Col>
              
              <Col xs={24} sm={12} md={8}>
                <Card style={{ 
                  borderRadius: '8px', 
                  background: 'rgba(255, 255, 255, 0.15)',
                  backdropFilter: 'blur(10px)',
                  border: 'none'
                }}>
                  <Statistic
                    title={<span style={{ color: 'rgba(255, 255, 255, 0.85)' }}>属性数量</span>}
                    value={Object.keys(event.properties).length}
                    valueStyle={{ color: 'white', fontWeight: 'bold' }}
                    prefix={<CodeOutlined style={{ color: 'white' }} />}
                  />
                </Card>
              </Col>
              
              <Col xs={24} sm={12} md={8}>
                <Card style={{ 
                  borderRadius: '8px', 
                  background: 'rgba(255, 255, 255, 0.15)',
                  backdropFilter: 'blur(10px)',
                  border: 'none'
                }}>
                  <Statistic
                    title={<span style={{ color: 'rgba(255, 255, 255, 0.85)' }}>元数据</span>}
                    value={event.metadataId ? '已关联' : '未关联'}
                    valueStyle={{ color: 'white', fontWeight: 'bold' }}
                    prefix={<DatabaseOutlined style={{ color: 'white' }} />}
                  />
                </Card>
              </Col>
            </Row>
          )}
        </div>
      </Card>
    </div>
  );

  return (
    <div className="event-details-page">
      {renderPageHeader()}
      
      {error && (
        <div style={{ marginBottom: 16 }}>
          <Alert
            message="错误"
            description={error}
            type="error"
            showIcon
          />
        </div>
      )}
      
      {loading ? (
        <Card
          style={{ 
            borderRadius: '12px',
            overflow: 'hidden',
            boxShadow: '0 4px 12px rgba(0,0,0,0.05)'
          }}
        >
          <Skeleton active paragraph={{ rows: 10 }} />
        </Card>
      ) : event ? (
        <Card
          style={{ 
            borderRadius: '12px', 
            overflow: 'hidden',
            boxShadow: '0 4px 12px rgba(0,0,0,0.05)'
          }}
          tabList={[
            { key: 'properties', tab: '属性数据' },
            { key: 'metadata', tab: '元数据信息', disabled: !metadata },
            { key: 'raw', tab: '原始数据' }
          ]}
          activeTabKey={activeTab}
          onTabChange={key => setActiveTab(key)}
        >
          {activeTab === 'properties' && (
            <>
              {Object.keys(event.properties).length > 0 ? (
                <Table
                  dataSource={Object.entries(event.properties).map(([key, value]) => ({
                    key,
                    value: value || 'null'
                  }))}
                  columns={[
                    {
                      title: '键',
                      dataIndex: 'key',
                      key: 'key',
                      width: '40%',
                      render: (text) => (
                        <div style={{ 
                          padding: '4px 8px', 
                          background: 'rgba(24, 144, 255, 0.05)',
                          borderRadius: '4px',
                          display: 'inline-block'
                        }}>
                          <Text strong type="success" code>{text}</Text>
                        </div>
                      )
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
                  bordered={false}
                  className="modern-table"
                />
              ) : (
                <Empty description="没有属性数据" image={Empty.PRESENTED_IMAGE_SIMPLE} />
              )}
            </>
          )}
          
          {activeTab === 'metadata' && metadata && (
            <div style={{ padding: '8px 0' }}>
              <Row gutter={[24, 16]}>
                <Col span={24}>
                  <Card 
                    title={
                      <div style={{ display: 'flex', alignItems: 'center' }}>
                        <DatabaseOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                        元数据详情
                      </div>
                    }
                    bordered={false}
                    extra={
                      <Link to={`/metadata/${event.metadataId}`}>
                        <Button type="link" icon={<LinkOutlined />}>
                          查看完整元数据
                        </Button>
                      </Link>
                    }
                  >
                    <Descriptions 
                      bordered 
                      column={{ xs: 1, sm: 2, md: 3 }}
                      size="small"
                      labelStyle={{ fontWeight: 500 }}
                    >
                      <Descriptions.Item label="系统ID" span={3}>
                        <Text ellipsis style={{ maxWidth: '100%' }} title={metadata.systemId}>
                          {metadata.systemId}
                        </Text>
                      </Descriptions.Item>
                      <Descriptions.Item label="插件版本">
                        <Tag color="blue">{metadata.pluginVersion}</Tag>
                      </Descriptions.Item>
                      <Descriptions.Item label="IDE版本">
                        {metadata.ideVersion}
                      </Descriptions.Item>
                      <Descriptions.Item label="操作系统">
                        {metadata.os} {metadata.osVersion}
                      </Descriptions.Item>
                      {metadata.javaVersion && (
                        <Descriptions.Item label="Java版本">
                          {metadata.javaVersion}
                        </Descriptions.Item>
                      )}
                      {metadata.projectType && (
                        <Descriptions.Item label="项目类型">
                          {metadata.projectType}
                        </Descriptions.Item>
                      )}
                    </Descriptions>
                  </Card>
                </Col>
                
                {metadata.additionalData && Object.keys(metadata.additionalData).length > 0 && (
                  <Col span={24}>
                    <Card 
                      title={
                        <div style={{ display: 'flex', alignItems: 'center' }}>
                          <InfoCircleOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                          附加数据
                        </div>
                      }
                      bordered={false}
                    >
                      <Table
                        dataSource={Object.entries(metadata.additionalData).map(([key, value]) => ({
                          key,
                          value: value || 'null'
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
                            width: '60%'
                          }
                        ]}
                        pagination={false}
                        bordered={false}
                        size="small"
                      />
                    </Card>
                  </Col>
                )}
              </Row>
            </div>
          )}
          
          {activeTab === 'raw' && (
            <div style={{ padding: '16px 0' }}>
              <Card bordered={false}>
                <pre style={{ 
                  whiteSpace: 'pre-wrap', 
                  wordBreak: 'break-word',
                  backgroundColor: '#f5f5f5',
                  padding: '16px',
                  borderRadius: '8px',
                  fontSize: '13px',
                  margin: 0,
                  maxHeight: '500px',
                  overflow: 'auto'
                }}>
                  {JSON.stringify({ event, metadata }, null, 2)}
                </pre>
              </Card>
            </div>
          )}
        </Card>
      ) : null}
    </div>
  );
};

export default EventDetails; 