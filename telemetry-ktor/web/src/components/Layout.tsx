import React, { useState, useEffect, useMemo } from 'react';
import { Layout, Menu, theme, Button, Dropdown, Space, Avatar, Badge, Tooltip, Typography } from 'antd';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import type { ItemType } from 'antd/es/menu/interface';
import type { MenuProps } from 'antd';
import {
  DashboardOutlined,
  BarChartOutlined,
  DatabaseOutlined,
  AppstoreOutlined,
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
  FileTextOutlined,
  UsergroupAddOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  BellOutlined,
  QuestionCircleOutlined,
  GithubOutlined,
  ApiOutlined,
  CloudOutlined,
  ToolOutlined,
  BookOutlined,
  SecurityScanOutlined,
  HomeOutlined,
  RocketOutlined
} from '@ant-design/icons';
import { getUserData, logoutWithJWT } from '../services/auth';
import '../styles/index.css';

const { Header, Content, Footer, Sider } = Layout;
const { Text } = Typography;

interface AppLayoutProps {
  children: React.ReactNode;
}

// 创建自定义图标组件，确保SVG居中
const CustomIcon = ({ icon }: { icon: React.ReactNode }) => (
  <div className="custom-menu-icon">
    {icon}
  </div>
);

// 路径映射配置
const PATH_MAP: Record<string, { name: string, key: string, icon: React.ReactNode }> = {
  '/': { name: '仪表盘', key: '1', icon: <DashboardOutlined /> },
  '/events': { name: '事件列表', key: '2', icon: <AppstoreOutlined /> },
  '/metadata': { name: '元数据', key: '3', icon: <DatabaseOutlined /> },
  '/reports': { name: '统计报表', key: '4', icon: <BarChartOutlined /> },
  '/users': { name: '用户管理', key: '5', icon: <UsergroupAddOutlined /> },
  '/settings': { name: '系统设置', key: '6', icon: <SettingOutlined /> },
  '/privacy-policy': { name: '隐私政策', key: '7', icon: <FileTextOutlined /> },
  '/api-docs': { name: 'API文档', key: '8', icon: <ApiOutlined /> },
  '/system': { name: '系统监控', key: '9', icon: <CloudOutlined /> },
  '/tools': { name: '开发工具', key: '10', icon: <ToolOutlined /> },
  '/help': { name: '帮助中心', key: '11', icon: <BookOutlined /> },
  '/security': { name: '安全中心', key: '12', icon: <SecurityScanOutlined /> },
};

const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  const [collapsed, setCollapsed] = useState(false);
  const [username, setUsername] = useState('管理员');
  const [displayName, setDisplayName] = useState('');
  const [role, setRole] = useState('');
  const location = useLocation();
  const navigate = useNavigate();
  
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();

  // 获取用户数据
  useEffect(() => {
    const user = getUserData();
    if (user) {
      setUsername(user.username || '管理员');
      setDisplayName(user.displayName || '');
      setRole(user.role || '');
    }
  }, []);
  
  // 响应式布局：当窗口宽度小于768px时自动折叠菜单
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth < 768 && !collapsed) {
        setCollapsed(true);
      }
    };
    
    window.addEventListener('resize', handleResize);
    handleResize(); // 初始检查
    
    return () => {
      window.removeEventListener('resize', handleResize);
    };
  }, [collapsed]);

  // 确定当前选中的菜单项
  const selectedKey = useMemo(() => {
    const pathname = location.pathname;
    
    // 先检查完全匹配
    if (PATH_MAP[pathname]) {
      return PATH_MAP[pathname].key;
    }
    
    // 再检查前缀匹配
    for (const path in PATH_MAP) {
      if (path !== '/' && pathname.startsWith(path)) {
        return PATH_MAP[path].key;
      }
    }
    
    return '1'; // 默认为仪表盘
  }, [location.pathname]);

  // 获取当前页面标题
  const currentPageTitle = useMemo(() => {
    const pathname = location.pathname;
    
    // 先检查完全匹配
    if (PATH_MAP[pathname]) {
      return PATH_MAP[pathname].name;
    }
    
    // 再检查前缀匹配
    for (const path in PATH_MAP) {
      if (path !== '/' && pathname.startsWith(path)) {
        return PATH_MAP[path].name;
      }
    }
    
    return '仪表盘'; // 默认为仪表盘
  }, [location.pathname]);
  
  // 处理登出
  const handleLogout = async () => {
    await logoutWithJWT();
    navigate('/login');
  };

  // 获取角色显示名称和颜色
  const getRoleDisplayName = () => {
    switch (role.toLowerCase()) {
      case 'admin': return '管理员';
      case 'user': return '普通用户';
      default: return role || '访客';
    }
  };

  const getRoleColor = () => {
    switch (role.toLowerCase()) {
      case 'admin': return 'blue';
      case 'user': return 'green';
      default: return 'default';
    }
  };

  // 生成菜单项
  const generateMenuItems = useMemo(() => {
    const items: ItemType[] = [];
    
    // 添加主菜单项
    Object.entries(PATH_MAP).forEach(([path, { name, key, icon }]) => {
      if (key === '5' || key === '8') {
        // 在用户管理和API文档前添加分隔线
        items.push({ type: 'divider' });
      }
      
      items.push({
        key,
        icon: <CustomIcon icon={icon} />,
        label: collapsed ? (
          <Tooltip placement="right" title={name}>
            <Link to={path}>{name}</Link>
          </Tooltip>
        ) : (
          <Link to={path}>{name}</Link>
        )
      });
    });
    
    return items;
  }, [collapsed]);

  // 用户下拉菜单项
  const userMenuItems: MenuProps['items'] = [
    {
      key: '1',
      label: (
        <div className="user-dropdown-info">
          <div className="user-name">{displayName || username}</div>
          <div className="user-role">
            <Badge 
              color={getRoleColor()} 
              text={getRoleDisplayName()} 
            />
          </div>
        </div>
      ),
      disabled: true,
      style: { cursor: 'default' }
    },
    { type: 'divider' },
    {
      key: '2',
      icon: <UserOutlined />,
      label: '个人中心',
      onClick: () => navigate('/settings/profile')
    },
    {
      key: '3',
      icon: <SettingOutlined />,
      label: '个人设置',
      onClick: () => navigate('/settings')
    },
    { type: 'divider' },
    {
      key: '4',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
      danger: true
    }
  ];

  return (
    <Layout className="app-layout modern-layout" hasSider>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        className="app-sider modern-sider"
        width={240}
        style={{
          boxShadow: '0 2px 20px rgba(0, 0, 0, 0.08)',
          zIndex: 10,
          position: 'fixed',
          height: '100vh',
          left: 0,
          top: 0,
          bottom: 0,
        }}
        trigger={null}
      >
        <div className="logo modern-logo">
          <div className="logo-content">
            {collapsed ? (
              <RocketOutlined className="logo-icon-collapsed" />
            ) : (
              <>
                <RocketOutlined className="logo-icon" />
                <span className="logo-text">CangJie</span>
                <span className="logo-badge">Admin</span>
              </>
            )}
          </div>
        </div>
        
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          className="app-menu modern-menu"
          items={generateMenuItems}
          inlineCollapsed={collapsed}
        />
        
        {!collapsed && (
          <div className="sider-footer modern-footer">
            <div className="sider-footer-content">
              <Button 
                type="text" 
                icon={<MenuFoldOutlined />}
                onClick={() => setCollapsed(true)}
                className="collapse-btn"
              />
              <Text type="secondary" style={{ fontSize: '12px' }}>
                CangJie v1.0.0
              </Text>
            </div>
          </div>
        )}
      </Sider>
      
      <Layout style={{ 
        marginLeft: collapsed ? 80 : 240, 
        transition: 'margin-left 0.2s cubic-bezier(0.645, 0.045, 0.355, 1)',
        minHeight: '100vh'
      }}>
        <Header 
          className="app-header modern-header"
          style={{ 
            padding: 0, 
            background: colorBgContainer,
            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)',
            position: 'sticky',
            top: 0,
            zIndex: 9,
            width: '100%',
            height: '64px',
            lineHeight: '64px',
            transition: 'all 0.2s'
          }}
        >
          <div className="header-content">
            <div className="header-left">
              <Tooltip title={collapsed ? "展开菜单" : "收起菜单"}>
                <Button 
                  type="text"
                  icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                  onClick={() => setCollapsed(!collapsed)}
                  className="menu-trigger"
                />
              </Tooltip>
              <div className="breadcrumb">
                <HomeOutlined />
                <span className="breadcrumb-separator">/</span>
                <span className="current-page">{currentPageTitle}</span>
              </div>
            </div>
            
            <div className="header-right">
              <Space size={16}>
                <Tooltip title="帮助文档">
                  <Button 
                    type="text" 
                    icon={<QuestionCircleOutlined />} 
                    onClick={() => window.open('/docs', '_blank')}
                  />
                </Tooltip>
                
                <Tooltip title="GitHub">
                  <Button 
                    type="text" 
                    icon={<GithubOutlined />} 
                    onClick={() => window.open('https://github.com/your-repo/cangjie', '_blank')}
                  />
                </Tooltip>
                
                <Tooltip title="通知">
                  <Badge count={3} size="small" offset={[2, -2]}>
                    <Button type="text" icon={<BellOutlined />} />
                  </Badge>
                </Tooltip>
                
                <Dropdown menu={{ items: userMenuItems }} trigger={['click']}>
                  <div className="user-dropdown">
                    <Avatar 
                      icon={<UserOutlined />} 
                      className="user-avatar"
                      style={{ backgroundColor: '#1890ff' }}
                    />
                    {!collapsed && (
                      <span className="user-name-display">{displayName || username}</span>
                    )}
                  </div>
                </Dropdown>
              </Space>
            </div>
          </div>
        </Header>
        
        <Content className="app-content modern-content">
          <div
            className="content-container"
            style={{
              background: colorBgContainer,
              borderRadius: borderRadiusLG,
              boxShadow: '0 2px 8px rgba(0, 0, 0, 0.06)',
              padding: '24px',
              margin: '16px',
              minHeight: 'calc(100vh - 64px - 32px - 69px)'
            }}
          >
            {children}
          </div>
        </Content>
        
        <Footer className="app-footer modern-footer">
          <div className="footer-content">
            <div className="copyright">CangJie Telemetry Admin Dashboard ©{new Date().getFullYear()} Created by LinQingYing</div>
            <div className="footer-links">
              <a href="/about" target="_blank" rel="noopener noreferrer">关于我们</a>
              <a href="/privacy-policy" target="_blank" rel="noopener noreferrer">隐私政策</a>
              <a href="/terms" target="_blank" rel="noopener noreferrer">服务条款</a>
              <a href="/feedback" target="_blank" rel="noopener noreferrer">反馈建议</a>
            </div>
          </div>
        </Footer>
      </Layout>
    </Layout>
  );
};

export default AppLayout; 