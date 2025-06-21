import React, { useState, useEffect } from 'react';
import { 
  Row, Col, Card, Table, Typography, Spin, 
  Avatar, Badge, Tag, List, Button, Space,
  Empty, Alert, Tooltip
} from 'antd';
import { 
  DashboardOutlined, DatabaseOutlined, BarChartOutlined, 
  SettingOutlined, UserOutlined, InfoCircleOutlined,
  ArrowRightOutlined, ClockCircleOutlined, CalendarOutlined,
  LinkOutlined, AppstoreOutlined, FileTextOutlined,
  CheckCircleOutlined, TeamOutlined
} from '@ant-design/icons';
import { Link } from 'react-router-dom';
import type { ApiResponse, DashboardData } from '../types';
import { getDashboardData } from '../services/api';
import { getUserData } from '../services/auth';
import SystemStatusOverview from '../components/SystemStatusOverview';
import StatisticCard from '../components/StatisticCard';

const { Title, Text, Paragraph } = Typography;

const Dashboard: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(true);
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [userData, setUserData] = useState<any>(null);
  const [welcomeMessage, setWelcomeMessage] = useState<string>('');

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

    // 获取用户数据
    const user = getUserData();
    if (user) {
      setUserData(user);
      
      // 根据时间设置欢迎消息
      const hour = new Date().getHours();
      if (hour < 6) {
        setWelcomeMessage('夜深了');
      } else if (hour < 9) {
        setWelcomeMessage('早上好');
      } else if (hour < 12) {
        setWelcomeMessage('上午好');
      } else if (hour < 14) {
        setWelcomeMessage('中午好');
      } else if (hour < 18) {
        setWelcomeMessage('下午好');
      } else if (hour < 22) {
        setWelcomeMessage('晚上好');
      } else {
        setWelcomeMessage('夜深了');
      }
    }

    fetchDashboardData();
  }, []);

  const metadataColumns = [
    {
      title: '插件版本',
      dataIndex: 'pluginVersion',
      key: 'pluginVersion',
      width: 120,
      render: (text: string) => <Tag color="blue">{text}</Tag>
    },
    {
      title: 'IDE版本',
      dataIndex: 'ideVersion',
      key: 'ideVersion',
      width: 120,
    },
    {
      title: '操作系统',
      key: 'os',
      render: (record: any) => {
        const osColor = record.os.includes('Windows') ? 'blue' : 
                        record.os.includes('Mac') ? 'cyan' : 
                        record.os.includes('Linux') ? 'green' : 'default';
        return (
          <Space>
            <Tag color={osColor}>{record.os}</Tag>
            <Text type="secondary" style={{ fontSize: '12px' }}>{record.osVersion}</Text>
          </Space>
        );
      },
      width: 150,
    },
    {
      title: '时间',
      dataIndex: 'formattedTime',
      key: 'formattedTime',
      width: 150,
      render: (text: string) => (
        <span>
          <ClockCircleOutlined style={{ marginRight: 5, color: '#1890ff' }} />
          {text}
        </span>
      )
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (record: any) => (
        <Link to={`/metadata/${record._id}`}>
          <Button type="link" size="small" icon={<ArrowRightOutlined />}>
            详情
          </Button>
        </Link>
      )
    }
  ];

  // 渲染管理员信息卡片
  const renderUserInfoCard = () => (
    <Card
      title={
        <span>
          <UserOutlined style={{ marginRight: 8 }} />
          管理员信息
        </span>
      }
      style={{ marginBottom: 16, borderRadius: '1rem', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}
      extra={
        <Link to="/settings/profile">
          <Button type="link" size="small">编辑</Button>
        </Link>
      }
    >
      {userData ? (
        <>
          <div style={{ display: 'flex', alignItems: 'center', marginBottom: 24 }}>
            <Avatar 
              size={80} 
              icon={<UserOutlined />} 
              style={{ 
                backgroundColor: 'rgba(24, 144, 255, 0.1)', 
                color: '#1890ff',
                marginRight: 16,
                boxShadow: '0 4px 8px rgba(24, 144, 255, 0.2)'
              }} 
            />
            <div>
              <div style={{ marginBottom: 8 }}>
                <Title level={4} style={{ margin: 0 }}>{userData.displayName || userData.username}</Title>
                <div style={{ display: 'flex', alignItems: 'center', marginTop: 4 }}>
                  <Badge status="success" />
                  <Text type="secondary" style={{ marginLeft: 8 }}>在线</Text>
                </div>
              </div>
              <Tag color="blue">{userData.role?.toUpperCase() || '管理员'}</Tag>
            </div>
          </div>
          
          <List
            size="small"
            bordered
            style={{ borderRadius: 8 }}
            dataSource={[
              {
                title: '上次登录时间',
                value: userData.lastLogin || '-',
                icon: <ClockCircleOutlined style={{ color: '#1890ff' }} />
              },
              {
                title: '创建时间',
                value: userData.createdAt || '-',
                icon: <CalendarOutlined style={{ color: '#52c41a' }} />
              }
            ]}
            renderItem={item => (
              <List.Item style={{ display: 'flex', justifyContent: 'space-between' }}>
                <div>
                  {item.icon} <Text style={{ marginLeft: 8 }}>{item.title}</Text>
                </div>
                <Tag color="default">{item.value}</Tag>
              </List.Item>
            )}
          />
          
          <div style={{ marginTop: 16, textAlign: 'center' }}>
            <Text type="secondary">{welcomeMessage}，{userData.displayName || userData.username}！</Text>
          </div>
        </>
      ) : (
        <div style={{ textAlign: 'center', padding: '20px 0' }}>
          <Empty description="无法获取管理员信息" />
        </div>
      )}
    </Card>
  );

  // 渲染快速链接卡片
  const renderQuickLinksCard = () => (
    <Card
      title={
        <span>
          <LinkOutlined style={{ marginRight: 8 }} />
          快速链接
        </span>
      }
      style={{ borderRadius: '1rem', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}
    >
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Link to="/events">
            <Button 
              type="default" 
              style={{ 
                width: '100%', 
                borderRadius: 8, 
                borderWidth: 2, 
                height: 'auto', 
                padding: '12px 0',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 8
              }}
              className="quick-link-button"
            >
              <AppstoreOutlined style={{ fontSize: 24 }} />
              <div>事件列表</div>
            </Button>
          </Link>
        </Col>
        <Col span={12}>
          <Link to="/metadata">
            <Button 
              type="default" 
              style={{ 
                width: '100%', 
                borderRadius: 8, 
                borderWidth: 2,
                height: 'auto', 
                padding: '12px 0',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 8
              }}
              className="quick-link-button"
            >
              <DatabaseOutlined style={{ fontSize: 24 }} />
              <div>元数据</div>
            </Button>
          </Link>
        </Col>
        <Col span={12}>
          <Link to="/reports">
            <Button 
              type="default" 
              style={{ 
                width: '100%', 
                borderRadius: 8, 
                borderWidth: 2,
                height: 'auto', 
                padding: '12px 0',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 8
              }}
              className="quick-link-button"
            >
              <BarChartOutlined style={{ fontSize: 24 }} />
              <div>统计报表</div>
            </Button>
          </Link>
        </Col>
        <Col span={12}>
          <Link to="/settings">
            <Button 
              type="default" 
              style={{ 
                width: '100%', 
                borderRadius: 8, 
                borderWidth: 2,
                height: 'auto', 
                padding: '12px 0',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 8
              }}
              className="quick-link-button"
            >
              <SettingOutlined style={{ fontSize: 24 }} />
              <div>系统设置</div>
            </Button>
          </Link>
        </Col>
      </Row>
    </Card>
  );

  return (
    <div className="dashboard-container">
      {/* 欢迎横幅 */}
      <Card 
        style={{ 
          marginBottom: 24, 
          borderRadius: '1rem', 
          backgroundImage: 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)',
          boxShadow: '0 4px 12px rgba(24, 144, 255, 0.15)',
          border: 'none'
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <Title level={3} style={{ color: 'white', margin: 0 }}>
              {welcomeMessage}，{userData?.displayName || userData?.username || '管理员'}！
            </Title>
            <Paragraph style={{ color: 'rgba(255, 255, 255, 0.85)', margin: '8px 0 0 0' }}>
              欢迎使用 CangJie 遥测管理系统，今天是 {new Date().toLocaleDateString('zh-CN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
            </Paragraph>
          </div>
          <div style={{ display: 'flex', gap: 16 }}>
            <Tooltip title="查看事件列表">
              <Link to="/events">
                <Button type="primary" ghost icon={<AppstoreOutlined />}>
                  事件列表
                </Button>
              </Link>
            </Tooltip>
            <Tooltip title="查看统计报表">
              <Link to="/reports">
                <Button type="primary" ghost icon={<BarChartOutlined />}>
                  统计报表
                </Button>
              </Link>
            </Tooltip>
          </div>
        </div>
      </Card>
      
      <Title level={4} style={{ marginBottom: 16 }}>
        <DashboardOutlined style={{ marginRight: 8 }} />
        数据概览
      </Title>
      
      {loading ? (
        <div style={{ textAlign: 'center', padding: '50px' }}>
          <Spin size="large" />
        </div>
      ) : error ? (
        <Alert
          message="获取数据失败"
          description={error}
          type="error"
          showIcon
          style={{ marginBottom: 24 }}
        />
      ) : (
        <>
          <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
            <Col xs={24} sm={12} lg={6}>
              <StatisticCard
                title="总事件数"
                value={dashboardData?.totalEvents || 0}
                icon={<DashboardOutlined />}
                color="primary"
                link="/events"
                subtitle={`今日新增: +${dashboardData?.todayNewEvents || 0}`}
                trend="up"
              />
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <StatisticCard
                title="总元数据数"
                value={dashboardData?.totalMetadata || 0}
                icon={<DatabaseOutlined />}
                color="success"
                link="/metadata"
                subtitle={`今日新增: +${dashboardData?.todayNewMetadata || 0}`}
                trend="up"
              />
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <StatisticCard
                title="活跃用户"
                value={dashboardData?.uniqueUsers || 128}
                icon={<TeamOutlined />}
                color="warning"
                link="/reports"
                subtitle={`较昨日: ${dashboardData?.userGrowth || '+5%'}`}
                trend="up"
              />
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <StatisticCard
                title="系统状态"
                value={100}
                icon={<CheckCircleOutlined />}
                color="danger"
                link="/settings"
                subtitle="运行正常"
                trend="stable"
              />
            </Col>
          </Row>

          <Row gutter={16}>
            <Col xs={24} lg={16}>
              <Card 
                title={
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>
                      <DatabaseOutlined style={{ marginRight: 8 }} />
                      最近添加的元数据
                    </span>
                    <Link to="/metadata">
                      <Button type="primary" size="small">
                        查看全部 <ArrowRightOutlined />
                      </Button>
                    </Link>
                  </div>
                }
                style={{ 
                  marginBottom: 16, 
                  borderRadius: '1rem',
                  boxShadow: '0 4px 12px rgba(0,0,0,0.05)'
                }}
              >
                {dashboardData?.recentMetadata && dashboardData.recentMetadata.length > 0 ? (
                  <Table
                    dataSource={dashboardData?.recentMetadata || []}
                    columns={metadataColumns}
                    rowKey="_id"
                    pagination={false}
                    size="middle"
                    className="dashboard-table"
                  />
                ) : (
                  <Empty description="暂无元数据" />
                )}
              </Card>
              
              <SystemStatusOverview
                systemStatus={dashboardData?.systemStatus || { cpu: 45, memory: 30, disk: 65, status: 'normal' }}
                lastSyncTime={dashboardData?.lastSyncTime || '未知'}
                loading={loading}
              />
            </Col>
            <Col xs={24} lg={8}>
              <div className="dashboard-sidebar">
                {renderUserInfoCard()}
                {renderQuickLinksCard()}
              </div>
            </Col>
          </Row>
        </>
      )}
    </div>
  );
};

export default Dashboard; 