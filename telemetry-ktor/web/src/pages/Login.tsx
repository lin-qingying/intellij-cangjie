import React, { useState, useEffect } from 'react';
import { Form, Input, Button, Card, message, Checkbox, Space, Alert } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import { loginWithJWT, isAuthenticated } from '../services/auth';

interface LoginForm {
  username: string;
  password: string;
}

const Login: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [devMode, setDevMode] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();
  const location = useLocation();
  
  // 如果已经登录，重定向到仪表盘
  useEffect(() => {
    if (isAuthenticated()) {
      navigate('/dashboard');
    }
  }, [navigate]);

  const handleSubmit = async (values: LoginForm) => {
    try {
      setLoading(true);
      setError(null);
      
      // Development mode bypass
      if (devMode) {
        localStorage.setItem('isAuthenticated', 'true');
        localStorage.setItem('user', JSON.stringify({ username: values.username || 'admin' }));
        message.success('开发模式登录成功');
        navigate('/dashboard');
        return;
      }
      
      const success = await loginWithJWT(values.username, values.password);
      
      if (success) {
        message.success('登录成功');
        // 获取重定向URL，如果没有则默认到仪表盘
        const from = location.state?.from?.pathname || '/dashboard';
        navigate(from);
      }
    } catch (error: any) {
      setError(error.message || '登录失败，请稍后重试');
      console.error('Login error:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      height: '100vh',
      background: '#f0f2f5'
    }}>
      <Card 
        title="CangJie 遥测管理系统" 
        style={{ width: 400, boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}
        headStyle={{ textAlign: 'center', fontSize: '20px' }}
      >
        {error && (
          <Alert
            message="登录错误"
            description={error}
            type="error"
            showIcon
            closable
            style={{ marginBottom: 16 }}
            onClose={() => setError(null)}
          />
        )}
        
        <Form
          name="login"
          initialValues={{ remember: true }}
          onFinish={handleSubmit}
          layout="vertical"
        >
          <Form.Item
            name="username"
            rules={[{ required: !devMode, message: '请输入用户名' }]}
          >
            <Input 
              prefix={<UserOutlined />} 
              placeholder="用户名" 
              size="large"
              disabled={loading}
              autoComplete="username"
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: !devMode, message: '请输入密码' }]}
          >
            <Input.Password 
              prefix={<LockOutlined />} 
              placeholder="密码" 
              size="large"
              disabled={loading}
              autoComplete="current-password"
            />
          </Form.Item>
          
          <Form.Item>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Checkbox 
                checked={devMode} 
                onChange={(e) => setDevMode(e.target.checked)}
                disabled={loading}
              >
                开发模式 (跳过后端验证)
              </Checkbox>
              
              <Button 
                type="primary" 
                htmlType="submit" 
                loading={loading}
                style={{ width: '100%' }}
                size="large"
              >
                登录
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default Login; 