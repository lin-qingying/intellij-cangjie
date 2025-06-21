import React, { useState, useEffect } from 'react';
import {
  Card, Typography, Button, Tag, Descriptions, Space,
  Row, Col, Divider, message, Spin, Alert, Breadcrumb, Statistic
} from 'antd';
import {
  DatabaseOutlined, DesktopOutlined, ClockCircleOutlined,
  ArrowLeftOutlined, InfoCircleOutlined, AppstoreOutlined,
  CodeOutlined, LaptopOutlined, LinkOutlined, FileTextOutlined
} from '@ant-design/icons';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { getMetadataById } from '../services/api';
import { API } from '../constants';
import type { ApiResponse, TelemetryMetadata } from '../types';

const { Title, Text } = Typography;

interface MetadataDetailsParams {
  id: string;
}

const MetadataDetails: React.FC = () => {
  const { id } = useParams<keyof MetadataDetailsParams>() as MetadataDetailsParams;
  const navigate = useNavigate();
  const [loading, setLoading] = useState<boolean>(true);
  const [metadata, setMetadata] = useState<TelemetryMetadata | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [formattedTime, setFormattedTime] = useState<string>('');
  const [eventsCount, setEventsCount] = useState<number>(0);

  // 获取元数据详情
  useEffect(() => {
    const fetchMetadataDetails = async () => {
      try {
        setLoading(true);
        
        // 获取元数据详情
        const response = await getMetadataById(id);
        const result = response.data as ApiResponse<{
          metadata: TelemetryMetadata;
          formattedTime: string;
          eventsCount: number;
        }>;
        
        if (result.success && result.data) {
          setMetadata(result.data.metadata);
          setFormattedTime(result.data.formattedTime);
          setEventsCount(result.data.eventsCount);
          setError(null);
        } else {
          setError(result.message || '获取元数据详情失败');
        }
      } catch (err: any) {
        setError(err.message || '获取元数据详情失败');
      } finally {
        setLoading(false);
      }
    };
    
    fetchMetadataDetails();
  }, [id]);

  // 获取操作系统标签颜色
  const getOsBadgeColor = (os?: string) => {
    if (!os) return 'default';
    
    if (os.includes('Windows')) return 'blue';
    if (os.includes('Mac')) return 'cyan';
    if (os.includes('Linux')) return 'green';
    return 'default';
  };

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Breadcrumb items={[
          { title: <Link to="/metadata">元数据列表</Link> },
          { title: '元数据详情' }
        ]} />
      </div>
      
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={2}>
          <DatabaseOutlined style={{ marginRight: 8 }} />
          元数据详情
        </Title>
        <Space>
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate('/metadata')}
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
      ) : metadata ? (
        <Card
          style={{ borderRadius: '1rem', overflow: 'hidden' }}
          title={
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <Text strong>系统 ID: </Text>
                <Text>{metadata.systemId}</Text>
              </div>
              <Tag icon={<ClockCircleOutlined />} color="blue">
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
                    <AppstoreOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                    插件信息
                  </div>
                }
                bordered={false}
                style={{ height: '100%' }}
              >
                <Descriptions column={{ xs: 1, sm: 1, md: 1 }} bordered>
                  <Descriptions.Item label="插件版本">
                    <Tag color="blue">{metadata.pluginVersion}</Tag>
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            </Col>
            
            <Col xs={24} md={12}>
              <Card
                title={
                  <div>
                    <LaptopOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                    IDE信息
                  </div>
                }
                bordered={false}
                style={{ height: '100%' }}
              >
                <Descriptions column={{ xs: 1, sm: 1, md: 1 }} bordered>
                  <Descriptions.Item label="IDE版本">{metadata.ideVersion}</Descriptions.Item>
                  <Descriptions.Item label="IDE构建号">{metadata.ideBuild}</Descriptions.Item>
                </Descriptions>
              </Card>
            </Col>
            
            <Col xs={24} md={12}>
              <Card
                title={
                  <div>
                    <DesktopOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                    系统信息
                  </div>
                }
                bordered={false}
                style={{ height: '100%' }}
              >
                <Descriptions column={{ xs: 1, sm: 1, md: 1 }} bordered>
                  <Descriptions.Item label="操作系统">
                    <Tag color={getOsBadgeColor(metadata.os)}>{metadata.os}</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="系统版本">{metadata.osVersion}</Descriptions.Item>
                  <Descriptions.Item label="Java版本">{metadata.javaVersion}</Descriptions.Item>
                </Descriptions>
              </Card>
            </Col>
            
            <Col xs={24} md={12}>
              <Card
                title={
                  <div>
                    <InfoCircleOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                    元数据信息
                  </div>
                }
                bordered={false}
                style={{ height: '100%' }}
              >
                <Descriptions column={{ xs: 1, sm: 1, md: 1 }} bordered>
                  <Descriptions.Item label="ID" span={1}>
                    <Text ellipsis style={{ maxWidth: '100%' }} title={metadata._id}>
                      {metadata._id}
                    </Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="时间戳">{formattedTime}</Descriptions.Item>
                  <Descriptions.Item label="关联事件">
                    <Statistic value={eventsCount} suffix="个事件" valueStyle={{ fontSize: '14px' }} />
                  </Descriptions.Item>
                </Descriptions>
              </Card>
            </Col>
          </Row>
          
          <div style={{ marginTop: 16, textAlign: 'right' }}>
            <Link to={`/events?metadataId=${metadata._id}`}>
              <Button type="primary" icon={<FileTextOutlined />}>
                查看相关事件
              </Button>
            </Link>
          </div>
        </Card>
      ) : null}
    </div>
  );
};

export default MetadataDetails; 