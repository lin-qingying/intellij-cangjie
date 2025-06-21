import React, { useState, useEffect } from 'react';
import {
  Card, Typography, Button, Tag, Descriptions, Table, Space,
  Row, Col, Divider, message, Spin, Modal, Breadcrumb, Alert
} from 'antd';
import {
  FileTextOutlined, InfoCircleOutlined, DatabaseOutlined,
  CodeOutlined, ArrowLeftOutlined, LinkOutlined
} from '@ant-design/icons';
import { ClockCircleOutlined } from '@ant-design/icons';
import { useParams, useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import { API } from '../constants';
import type { ApiResponse, TelemetryEvent, TelemetryMetadata } from '../types';
import api from "../services/api.ts";

const { Title, Text } = Typography;

interface EventDetailsParams {
  id: string;
}

const EventDetails: React.FC = () => {
  const { id } = useParams<keyof EventDetailsParams>() as EventDetailsParams;
  const navigate = useNavigate();
  const [loading, setLoading] = useState<boolean>(true);
  const [event, setEvent] = useState<TelemetryEvent | null>(null);
  const [metadata, setMetadata] = useState<TelemetryMetadata | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [formattedTime, setFormattedTime] = useState<string>('');

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
            setMetadata(result.data.metadata);
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

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Breadcrumb items={[
          { title: <Link to="/events">事件列表</Link> },
          { title: '事件详情' }
        ]} />
      </div>
      
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={2}>
          <FileTextOutlined style={{ marginRight: 8 }} />
          事件详情
        </Title>
        <Space>
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/events')}
          >
            返回列表
          </Button>
        </Space>
      </div>
      
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
        <div style={{ textAlign: 'center', padding: '50px' }}>
          <Spin size="large" />
        </div>
      ) : event ? (
        <Card
          style={{ borderRadius: '1rem', overflow: 'hidden' }}
          title={
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <Tag color={getCategoryColor(event.category)} style={{ marginRight: 8 }}>
                  {event.category}
                </Tag>
                {event.name}
              </div>
              <Tag icon={<ClockCircleOutlined />} color="default">
                {formattedTime || '未知时间'}
              </Tag>
            </div>
          }
        >
          <Row gutter={[16, 16]}>
            <Col xs={24} md={12}>
              <Card
                title={
                  <div>
                    <InfoCircleOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                    基本信息
                  </div>
                }
                bordered={false}
                style={{ height: '100%' }}
              >
                <Descriptions column={{ xs: 1, sm: 1, md: 1 }} bordered>
                  <Descriptions.Item label="ID" span={1}>
                    <Text ellipsis style={{ maxWidth: '100%' }} title={event.id}>
                      {event.id}
                    </Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="类别">
                    <Tag color={getCategoryColor(event.category)}>{event.category}</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="名称">{event.name}</Descriptions.Item>
                  <Descriptions.Item label="值">{event.value || '-'}</Descriptions.Item>
                </Descriptions>
              </Card>
            </Col>
            
            {event.metadataId && (
              <Col xs={24} md={12}>
                <Card
                  title={
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div>
                        <DatabaseOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                        关联元数据
                      </div>
                      <Link to={`/metadata/${event.metadataId}`}>
                        <LinkOutlined />
                      </Link>
                    </div>
                  }
                  bordered={false}
                  style={{ height: '100%' }}
                >
                  {metadata ? (
                    <Descriptions column={{ xs: 1, sm: 1, md: 1 }} bordered>
                      <Descriptions.Item label="系统ID">
                        <Text ellipsis style={{ maxWidth: '100%' }} title={metadata.systemId}>
                          {metadata.systemId}
                        </Text>
                      </Descriptions.Item>
                      <Descriptions.Item label="插件版本">{metadata.pluginVersion}</Descriptions.Item>
                      <Descriptions.Item label="IDE版本">{metadata.ideVersion}</Descriptions.Item>
                      <Descriptions.Item label="操作系统">{metadata.os} {metadata.osVersion}</Descriptions.Item>
                    </Descriptions>
                  ) : (
                    <div style={{ textAlign: 'center', padding: '20px 0' }}>
                      <Text type="secondary">无法加载关联元数据</Text>
                    </div>
                  )}
                </Card>
              </Col>
            )}
            
            <Col span={24}>
              <Card
                title={
                  <div>
                    <CodeOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                    属性数据
                  </div>
                }
                bordered={false}
              >
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
                        render: (text) => <Text strong type="success" code>{text}</Text>
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
                    bordered
                  />
                ) : (
                  <div style={{ textAlign: 'center', padding: '20px 0' }}>
                    <Text type="secondary">没有属性数据</Text>
                  </div>
                )}
              </Card>
            </Col>
          </Row>
        </Card>
      ) : null}
    </div>
  );
};

export default EventDetails; 