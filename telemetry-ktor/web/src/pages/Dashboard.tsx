import React, { useState, useEffect } from 'react';
import { Row, Col, Card, Statistic, Table, Typography, Spin } from 'antd';
import type { ApiResponse, DashboardData } from '../types';
import { getDashboardData } from '../services/api';

const { Title } = Typography;

const Dashboard: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(true);
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        setLoading(true);
        const response = await getDashboardData();
        const result = response.data as ApiResponse<DashboardData>;
        
        if (result.success && result.data) {
          setDashboardData(result.data);
          setError(null);
        } else {
          setError(result.message || '获取仪表盘数据失败');
        }
      } catch (err: any) {
        setError(err.message || '获取仪表盘数据失败');
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, []);

  const metadataColumns = [
    {
      title: '系统ID',
      dataIndex: 'systemId',
      key: 'systemId',
      width: 150,
      ellipsis: true,
    },
    {
      title: '插件版本',
      dataIndex: 'pluginVersion',
      key: 'pluginVersion',
      width: 100,
    },
    {
      title: 'IDE版本',
      dataIndex: 'ideVersion',
      key: 'ideVersion',
      width: 100,
    },
    {
      title: '操作系统',
      key: 'os',
      render: (record: any) => `${record.os} ${record.osVersion}`,
      width: 120,
    },
    {
      title: 'Java版本',
      dataIndex: 'javaVersion',
      key: 'javaVersion',
      width: 100,
    },
    {
      title: '时间',
      dataIndex: 'formattedTime',
      key: 'formattedTime',
      width: 150,
    },
  ];

  return (
    <div>
      <Title level={2}>仪表盘</Title>
      
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
          <Row gutter={16} style={{ marginBottom: 24 }}>
            <Col span={12}>
              <Card>
                <Statistic
                  title="事件总数"
                  value={dashboardData?.totalEvents || 0}
                  valueStyle={{ color: '#3f8600' }}
                />
              </Card>
            </Col>
            <Col span={12}>
              <Card>
                <Statistic
                  title="元数据总数"
                  value={dashboardData?.totalMetadata || 0}
                  valueStyle={{ color: '#1677ff' }}
                />
              </Card>
            </Col>
          </Row>

          <Card title="最近添加的元数据">
            <Table
              dataSource={dashboardData?.recentMetadata || []}
              columns={metadataColumns}
              rowKey="id"
              pagination={false}
              size="small"
            />
          </Card>
        </>
      )}
    </div>
  );
};

export default Dashboard; 