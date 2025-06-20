import React, { useState, useEffect } from 'react';
import { Layout, Menu, theme, Button, Dropdown, Space } from 'antd';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import {
  DashboardOutlined,
  BarChartOutlined,
  DatabaseOutlined,
  AppstoreOutlined,
  UserOutlined,
  LogoutOutlined
} from '@ant-design/icons';
import '../styles/index.css';

const { Header, Content, Footer, Sider } = Layout;

interface AppLayoutProps {
  children: React.ReactNode;
}

const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  const [collapsed, setCollapsed] = useState(false);
  const [username, setUsername] = useState('管理员');
  const location = useLocation();
  const navigate = useNavigate();
  
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();

  useEffect(() => {
    // Get username from localStorage
    try {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        const user = JSON.parse(userStr);
        if (user && user.username) {
          setUsername(user.username);
        }
      }
    } catch (e) {
      console.error('Error parsing user data:', e);
    }
  }, []);

  // 确定当前选中的菜单项
  const getSelectedKey = () => {
    const pathname = location.pathname;
    if (pathname.startsWith('/events')) return '2';
    if (pathname.startsWith('/metadata')) return '3';
    if (pathname.startsWith('/reports')) return '4';
    return '1'; // 默认为仪表盘
  };
  
  // 处理登出
  const handleLogout = () => {
    localStorage.removeItem('isAuthenticated');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <Layout className="app-layout">
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={(value) => setCollapsed(value)}
      >
        <div className="logo">
          {!collapsed ? 'CangJie Admin' : 'CJ'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          defaultSelectedKeys={[getSelectedKey()]}
          items={[
            {
              key: '1',
              icon: <DashboardOutlined />,
              label: <Link to="/">仪表盘</Link>,
            },
            {
              key: '2',
              icon: <AppstoreOutlined />,
              label: <Link to="/events">事件列表</Link>,
            },
            {
              key: '3',
              icon: <DatabaseOutlined />,
              label: <Link to="/metadata">元数据</Link>,
            },
            {
              key: '4',
              icon: <BarChartOutlined />,
              label: <Link to="/reports">统计报表</Link>,
            },
          ]}
        />
      </Sider>
      <Layout>
        <Header style={{ padding: 0, background: colorBgContainer }}>
          <div className="header-content">
            <div style={{ marginLeft: 16 }}>
              <span>CangJie 遥测管理后台</span>
            </div>
            <div style={{ marginRight: 16 }}>
              <Dropdown menu={{ 
                items: [
                  {
                    key: '1',
                    icon: <LogoutOutlined />,
                    label: '退出登录',
                    onClick: handleLogout
                  }
                ] 
              }}>
                <Space>
                  <Button type="text" icon={<UserOutlined />}>
                    {username}
                  </Button>
                </Space>
              </Dropdown>
            </div>
          </div>
        </Header>
        <Content style={{ margin: '0 16px' }}>
          <div
            style={{
              padding: 24,
              minHeight: 360,
              background: colorBgContainer,
              borderRadius: borderRadiusLG,
              margin: '16px 0',
            }}
          >
            {children}
          </div>
        </Content>
        <Footer style={{ textAlign: 'center' }}>
          CangJie Telemetry Admin Dashboard ©{new Date().getFullYear()} Created by LinQingYing
        </Footer>
      </Layout>
    </Layout>
  );
};

export default AppLayout; 