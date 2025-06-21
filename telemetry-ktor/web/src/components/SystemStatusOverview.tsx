import React from 'react';
import { Card, Row, Col, Progress, Statistic, Typography, Space, Tooltip } from 'antd';
import {
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  CloseCircleOutlined,
  InfoCircleOutlined,
  ClockCircleOutlined,
  SyncOutlined
} from '@ant-design/icons';

const { Text, Title } = Typography;

interface SystemStatusOverviewProps {
  systemStatus: {
    cpu: number;
    memory: number;
    disk: number;
    status: 'normal' | 'warning' | 'error';
  };
  lastSyncTime: string;
  loading?: boolean;
}

const SystemStatusOverview: React.FC<SystemStatusOverviewProps> = ({
  systemStatus,
  lastSyncTime,
  loading = false
}) => {
  const getStatusIcon = (status: 'normal' | 'warning' | 'error') => {
    switch (status) {
      case 'normal':
        return <CheckCircleOutlined style={{ color: '#52c41a' }} />;
      case 'warning':
        return <ExclamationCircleOutlined style={{ color: '#faad14' }} />;
      case 'error':
        return <CloseCircleOutlined style={{ color: '#f5222d' }} />;
      default:
        return <InfoCircleOutlined />;
    }
  };

  const getProgressStatus = (value: number) => {
    if (value >= 90) return 'exception';
    if (value >= 70) return 'warning';
    return 'normal';
  };

  const getProgressStrokeColor = (value: number) => {
    if (value >= 90) return '#f5222d';
    if (value >= 70) return '#faad14';
    return '#52c41a';
  };

  return (
    <Card
      title={
        <Space>
          <SyncOutlined spin={loading} />
          系统状态概览
        </Space>
      }
      extra={
        <Tooltip title="最后同步时间">
          <Space>
            <ClockCircleOutlined />
            <Text type="secondary">{lastSyncTime}</Text>
          </Space>
        </Tooltip>
      }
      style={{ 
        borderRadius: '1rem', 
        boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
        height: '100%'
      }}
    >
      <div style={{ marginBottom: 16 }}>
        <Space align="center">
          {getStatusIcon(systemStatus.status)}
          <Text strong>
            系统状态: {
              systemStatus.status === 'normal' ? '正常' :
              systemStatus.status === 'warning' ? '注意' : '异常'
            }
          </Text>
        </Space>
      </div>

      <Row gutter={[16, 24]}>
        <Col span={24}>
          <div style={{ marginBottom: 8 }}>
            <Space>
              <Text>CPU 使用率</Text>
              <Text type="secondary">{systemStatus.cpu}%</Text>
            </Space>
          </div>
          <Progress 
            percent={systemStatus.cpu} 
            status={getProgressStatus(systemStatus.cpu) as any}
            strokeColor={getProgressStrokeColor(systemStatus.cpu)}
            showInfo={false}
          />
        </Col>
        
        <Col span={24}>
          <div style={{ marginBottom: 8 }}>
            <Space>
              <Text>内存使用率</Text>
              <Text type="secondary">{systemStatus.memory}%</Text>
            </Space>
          </div>
          <Progress 
            percent={systemStatus.memory} 
            status={getProgressStatus(systemStatus.memory) as any}
            strokeColor={getProgressStrokeColor(systemStatus.memory)}
            showInfo={false}
          />
        </Col>
        
        <Col span={24}>
          <div style={{ marginBottom: 8 }}>
            <Space>
              <Text>磁盘使用率</Text>
              <Text type="secondary">{systemStatus.disk}%</Text>
            </Space>
          </div>
          <Progress 
            percent={systemStatus.disk} 
            status={getProgressStatus(systemStatus.disk) as any}
            strokeColor={getProgressStrokeColor(systemStatus.disk)}
            showInfo={false}
          />
        </Col>
      </Row>
    </Card>
  );
};

export default SystemStatusOverview; 