import React, { useState, useEffect, useMemo } from 'react';
import { 
  Layout, Menu, theme, Button, Dropdown, Space, Avatar, 
  Badge, Tooltip, Typography, Input, Drawer, Tabs, List,
  Divider, Tag, Skeleton, Switch, ConfigProvider
} from 'antd';
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
  RocketOutlined,
  SearchOutlined,
  CloseOutlined,
  CheckOutlined,
  InfoCircleOutlined,
  ThunderboltOutlined,
  TeamOutlined,
  LineChartOutlined,
  CodeOutlined,
  GlobalOutlined,
  PieChartOutlined,
  CalendarOutlined,
  FundOutlined,
  ApartmentOutlined,
  ClockCircleOutlined,
  SunOutlined,
  MoonOutlined
} from '@ant-design/icons';
import { getUserData, logoutWithJWT } from '../services/auth';
import '../styles/index.css';

const { Header, Content, Footer, Sider } = Layout;
const { Text, Title } = Typography;
const { Search } = Input;
const { TabPane } = Tabs;

interface AppLayoutProps {
  children: React.ReactNode;
}

// 创建自定义图标组件，确保SVG居中
const CustomIcon = ({ icon }: { icon: React.ReactNode }) => (
  <div className="custom-menu-icon">
    {icon}
  </div>
);

// 菜单分类
const MENU_CATEGORIES = {
  OVERVIEW: '概览',
  DATA: '数据管理',
  SYSTEM: '系统管理',
  TOOLS: '工具和帮助'
};

// 路径映射配置
const PATH_MAP: Record<string, { 
  name: string, 
  key: string, 
  icon: React.ReactNode,
  category: string,
  description?: string
}> = {
  '/': { 
    name: '仪表盘', 
    key: '1', 
    icon: <DashboardOutlined />,
    category: MENU_CATEGORIES.OVERVIEW,
    description: '系统概览和关键指标'
  },
  '/events': { 
    name: '事件列表', 
    key: '2', 
    icon: <AppstoreOutlined />,
    category: MENU_CATEGORIES.DATA,
    description: '查看和管理所有事件数据'
  },
  '/metadata': { 
    name: '元数据', 
    key: '3', 
    icon: <DatabaseOutlined />,
    category: MENU_CATEGORIES.DATA,
    description: '查看和管理元数据信息'
  },
  '/reports': { 
    name: '统计报表', 
    key: '4', 
    icon: <BarChartOutlined />,
    category: MENU_CATEGORIES.DATA,
    description: '数据分析和报表生成'
  },
  '/analytics': { 
    name: '数据分析', 
    key: '13', 
    icon: <LineChartOutlined />,
    category: MENU_CATEGORIES.DATA,
    description: '高级数据分析和可视化'
  },
  '/users': { 
    name: '用户管理', 
    key: '5', 
    icon: <TeamOutlined />,
    category: MENU_CATEGORIES.SYSTEM,
    description: '管理系统用户和权限'
  },
  '/settings': { 
    name: '系统设置', 
    key: '6', 
    icon: <SettingOutlined />,
    category: MENU_CATEGORIES.SYSTEM,
    description: '配置系统参数和选项'
  },
  '/system': { 
    name: '系统监控', 
    key: '9', 
    icon: <CloudOutlined />,
    category: MENU_CATEGORIES.SYSTEM,
    description: '监控系统状态和性能'
  },
  '/integrations': { 
    name: '系统集成', 
    key: '14', 
    icon: <ApartmentOutlined />,
    category: MENU_CATEGORIES.SYSTEM,
    description: '配置第三方系统集成'
  },
  '/api-docs': { 
    name: 'API文档', 
    key: '8', 
    icon: <ApiOutlined />,
    category: MENU_CATEGORIES.TOOLS,
    description: 'API接口文档和示例'
  },
  '/tools': { 
    name: '开发工具', 
    key: '10', 
    icon: <ToolOutlined />,
    category: MENU_CATEGORIES.TOOLS,
    description: '开发者工具和实用功能'
  },
  '/help': { 
    name: '帮助中心', 
    key: '11', 
    icon: <BookOutlined />,
    category: MENU_CATEGORIES.TOOLS,
    description: '使用指南和帮助文档'
  },
  '/security': { 
    name: '安全中心', 
    key: '12', 
    icon: <SecurityScanOutlined />,
    category: MENU_CATEGORIES.SYSTEM,
    description: '安全设置和审计日志'
  },
  '/privacy-policy': { 
    name: '隐私政策', 
    key: '7', 
    icon: <FileTextOutlined />,
    category: MENU_CATEGORIES.TOOLS,
    description: '系统隐私政策说明'
  },
};

// 通知类型定义
interface Notification {
  id: string;
  title: string;
  message: string;
  type: 'info' | 'warning' | 'success' | 'error';
  time: string;
  read: boolean;
}

const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  const [collapsed, setCollapsed] = useState(false);
  const [username, setUsername] = useState('管理员');
  const [displayName, setDisplayName] = useState('');
  const [role, setRole] = useState('');
  const [darkMode, setDarkMode] = useState(false);
  const [searchVisible, setSearchVisible] = useState(false);
  const [notificationDrawerVisible, setNotificationDrawerVisible] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([
    {
      id: '1',
      title: '系统更新',
      message: '系统已更新到最新版本 v1.2.0',
      type: 'success',
      time: '10分钟前',
      read: false
    },
    {
      id: '2',
      title: '安全警告',
      message: '检测到异常登录尝试，请检查系统日志',
      type: 'warning',
      time: '2小时前',
      read: false
    },
    {
      id: '3',
      title: '数据同步完成',
      message: '元数据同步已完成，共更新52条记录',
      type: 'info',
      time: '昨天',
      read: true
    }
  ]);
  
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
    
    // 检查暗黑模式设置
    const savedDarkMode = localStorage.getItem('darkMode') === 'true';
    setDarkMode(savedDarkMode);
    if (savedDarkMode) {
      document.body.classList.add('dark-mode');
    } else {
      document.body.classList.remove('dark-mode');
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

  // 切换暗黑模式
  const toggleDarkMode = () => {
    const newDarkMode = !darkMode;
    setDarkMode(newDarkMode);
    localStorage.setItem('darkMode', String(newDarkMode));
    
    if (newDarkMode) {
      document.body.classList.add('dark-mode');
    } else {
      document.body.classList.remove('dark-mode');
    }
  };

  // 标记所有通知为已读
  const markAllNotificationsAsRead = () => {
    setNotifications(prevNotifications => 
      prevNotifications.map(notification => ({ ...notification, read: true }))
    );
  };

  // 删除通知
  const deleteNotification = (id: string) => {
    setNotifications(prevNotifications => 
      prevNotifications.filter(notification => notification.id !== id)
    );
  };

  // 生成菜单项
  const generateMenuItems = useMemo(() => {
    const categories: Record<string, ItemType[]> = {};
    
    // 初始化分类
    Object.values(MENU_CATEGORIES).forEach(category => {
      categories[category] = [];
    });
    
    // 按分类添加菜单项
    Object.entries(PATH_MAP).forEach(([path, { name, key, icon, category }]) => {
      categories[category].push({
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
    
    // 构建最终菜单项，添加分组
    const items: ItemType[] = [];
    
    // 添加分组和菜单项
    Object.entries(categories).forEach(([category, categoryItems], index) => {
      // 如果不是第一个分组，添加分隔线
      if (index > 0) {
        items.push({ type: 'divider' });
      }
      
      // 添加分组标题（仅在展开状态下显示）
      if (!collapsed) {
        items.push({
          type: 'group',
          label: category
        });
      }
      
      // 添加该分组下的菜单项
      items.push(...categoryItems);
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
    {
      key: '5',
      icon: darkMode ? <SunOutlined /> : <MoonOutlined />,
      label: darkMode ? '切换到亮色模式' : '切换到暗色模式',
      onClick: toggleDarkMode
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

  // 未读通知数量
  const unreadNotificationsCount = useMemo(() => {
    return notifications.filter(notification => !notification.read).length;
  }, [notifications]);

  // 渲染通知图标
  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'success':
        return <CheckOutlined style={{ color: '#52c41a' }} />;
      case 'warning':
        return <InfoCircleOutlined style={{ color: '#faad14' }} />;
      case 'error':
        return <CloseOutlined style={{ color: '#f5222d' }} />;
      default:
        return <InfoCircleOutlined style={{ color: '#1890ff' }} />;
    }
  };

  return (
    <ConfigProvider
      theme={{
        algorithm: darkMode ? theme.darkAlgorithm : theme.defaultAlgorithm,
      }}
    >
      <Layout className={`app-layout modern-layout ${darkMode ? 'dark-mode' : ''}`} hasSider>
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
                  <Tooltip title="全局搜索">
                    <Button 
                      type="text" 
                      icon={<SearchOutlined />} 
                      onClick={() => setSearchVisible(true)}
                      className="action-btn"
                    />
                  </Tooltip>
                  
                  <Tooltip title="帮助文档">
                    <Button 
                      type="text" 
                      icon={<QuestionCircleOutlined />} 
                      onClick={() => window.open('/docs', '_blank')}
                      className="action-btn"
                    />
                  </Tooltip>
                  
                  <Tooltip title="GitHub">
                    <Button 
                      type="text" 
                      icon={<GithubOutlined />} 
                      onClick={() => window.open('https://github.com/your-repo/cangjie', '_blank')}
                      className="action-btn"
                    />
                  </Tooltip>
                  
                  <Tooltip title="通知中心">
                    <Badge count={unreadNotificationsCount} size="small" offset={[2, -2]}>
                      <Button 
                        type="text" 
                        icon={<BellOutlined />} 
                        onClick={() => setNotificationDrawerVisible(true)}
                        className="action-btn"
                      />
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
        
        {/* 全局搜索抽屉 */}
        <Drawer
          title="全局搜索"
          placement="top"
          height={500}
          onClose={() => setSearchVisible(false)}
          open={searchVisible}
          bodyStyle={{ padding: '24px' }}
          headerStyle={{ borderBottom: '1px solid #f0f0f0' }}
        >
          <div style={{ maxWidth: '800px', margin: '0 auto' }}>
            <Search
              placeholder="搜索事件、元数据、用户等..."
              enterButton="搜索"
              size="large"
              onSearch={(value) => console.log(value)}
              autoFocus
              style={{ marginBottom: '24px' }}
            />
            
            <Tabs defaultActiveKey="1">
              <TabPane tab="事件" key="1">
                <Skeleton active />
              </TabPane>
              <TabPane tab="元数据" key="2">
                <Skeleton active />
              </TabPane>
              <TabPane tab="用户" key="3">
                <Skeleton active />
              </TabPane>
              <TabPane tab="文档" key="4">
                <Skeleton active />
              </TabPane>
            </Tabs>
            
            <div style={{ marginTop: '24px', textAlign: 'center' }}>
              <Text type="secondary">使用关键词搜索系统内的内容</Text>
            </div>
          </div>
        </Drawer>
        
        {/* 通知中心抽屉 */}
        <Drawer
          title={
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>通知中心</span>
              <Button type="link" onClick={markAllNotificationsAsRead}>
                全部标为已读
              </Button>
            </div>
          }
          placement="right"
          onClose={() => setNotificationDrawerVisible(false)}
          open={notificationDrawerVisible}
          width={380}
        >
          <Tabs defaultActiveKey="1">
            <TabPane tab="全部" key="1">
              <List
                itemLayout="horizontal"
                dataSource={notifications}
                renderItem={item => (
                  <List.Item
                    className={`notification-item ${item.read ? 'read' : 'unread'}`}
                    actions={[
                      <Button 
                        type="text" 
                        icon={<CloseOutlined />} 
                        size="small"
                        onClick={() => deleteNotification(item.id)}
                      />
                    ]}
                  >
                    <List.Item.Meta
                      avatar={
                        <Avatar icon={getNotificationIcon(item.type)} />
                      }
                      title={
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                          <Text strong={!item.read}>{item.title}</Text>
                          <Text type="secondary" style={{ fontSize: '12px' }}>{item.time}</Text>
                        </div>
                      }
                      description={item.message}
                    />
                  </List.Item>
                )}
                locale={{ emptyText: '暂无通知' }}
              />
            </TabPane>
            <TabPane tab="未读" key="2">
              <List
                itemLayout="horizontal"
                dataSource={notifications.filter(item => !item.read)}
                renderItem={item => (
                  <List.Item
                    className="notification-item unread"
                    actions={[
                      <Button 
                        type="text" 
                        icon={<CloseOutlined />} 
                        size="small"
                        onClick={() => deleteNotification(item.id)}
                      />
                    ]}
                  >
                    <List.Item.Meta
                      avatar={
                        <Avatar icon={getNotificationIcon(item.type)} />
                      }
                      title={
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                          <Text strong>{item.title}</Text>
                          <Text type="secondary" style={{ fontSize: '12px' }}>{item.time}</Text>
                        </div>
                      }
                      description={item.message}
                    />
                  </List.Item>
                )}
                locale={{ emptyText: '暂无未读通知' }}
              />
            </TabPane>
          </Tabs>
        </Drawer>
      </Layout>
    </ConfigProvider>
  );
};

export default AppLayout; 