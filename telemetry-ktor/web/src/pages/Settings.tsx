import React, { useState, useEffect } from 'react';
import {
  Card, Typography, Tabs, Form, Input, Button, Switch,
  Select, InputNumber, Divider, message, Spin, Alert
} from 'antd';
import { SaveOutlined, ReloadOutlined } from '@ant-design/icons';
import axios from 'axios';
import { API } from '../constants';

const { Title, Paragraph } = Typography;
const { TabPane } = Tabs;
const { Option } = Select;

interface SystemSettings {
  general: {
    siteName: string;
    siteDescription: string;
    adminEmail: string;
    dataRetentionDays: number;
    enableRegistration: boolean;
  };
  telemetry: {
    enableTelemetry: boolean;
    collectAnonymousData: boolean;
    sendErrorReports: boolean;
    dataRetentionPeriod: number;
  };
  security: {
    sessionTimeoutMinutes: number;
    maxLoginAttempts: number;
    passwordMinLength: number;
    requireStrongPasswords: boolean;
    jwtExpirationMinutes: number;
    refreshTokenExpirationDays: number;
  };
  notification: {
    enableEmailNotifications: boolean;
    smtpServer: string;
    smtpPort: number;
    smtpUsername: string;
    smtpPassword: string;
    smtpUseSsl: boolean;
    emailFrom: string;
  };
}

const Settings: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(true);
  const [saving, setSaving] = useState<boolean>(false);
  const [settings, setSettings] = useState<SystemSettings | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<string>('general');
  
  const [generalForm] = Form.useForm();
  const [telemetryForm] = Form.useForm();
  const [securityForm] = Form.useForm();
  const [notificationForm] = Form.useForm();

  // 获取系统设置
  const fetchSettings = async () => {
    try {
      setLoading(true);
      // 在实际应用中，这里应该调用API获取设置
      // 这里为了演示，使用模拟数据
      const mockSettings: SystemSettings = {
        general: {
          siteName: 'CangJie 遥测管理系统',
          siteDescription: '用于管理和分析 IntelliJ 插件遥测数据的平台',
          adminEmail: 'admin@cangnova.cn',
          dataRetentionDays: 90,
          enableRegistration: false,
        },
        telemetry: {
          enableTelemetry: true,
          collectAnonymousData: true,
          sendErrorReports: true,
          dataRetentionPeriod: 365,
        },
        security: {
          sessionTimeoutMinutes: 30,
          maxLoginAttempts: 5,
          passwordMinLength: 8,
          requireStrongPasswords: true,
          jwtExpirationMinutes: 60,
          refreshTokenExpirationDays: 7,
        },
        notification: {
          enableEmailNotifications: false,
          smtpServer: 'smtp.example.com',
          smtpPort: 587,
          smtpUsername: 'noreply@cangnova.cn',
          smtpPassword: '',
          smtpUseSsl: true,
          emailFrom: 'CangJie Telemetry <noreply@cangnova.cn>',
        },
      };
      
      setSettings(mockSettings);
      
      // 填充表单
      generalForm.setFieldsValue(mockSettings.general);
      telemetryForm.setFieldsValue(mockSettings.telemetry);
      securityForm.setFieldsValue(mockSettings.security);
      notificationForm.setFieldsValue(mockSettings.notification);
      
      setError(null);
    } catch (err: any) {
      setError(err.message || '获取系统设置失败');
    } finally {
      setLoading(false);
    }
  };

  // 初始加载
  useEffect(() => {
    fetchSettings();
  }, []);

  // 保存通用设置
  const saveGeneralSettings = async (values: any) => {
    try {
      setSaving(true);
      // 在实际应用中，这里应该调用API保存设置
      console.log('保存通用设置:', values);
      
      // 模拟API请求
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      message.success('通用设置保存成功');
      
      // 更新本地状态
      if (settings) {
        setSettings({
          ...settings,
          general: values,
        });
      }
    } catch (err: any) {
      message.error(err.message || '保存设置失败');
    } finally {
      setSaving(false);
    }
  };

  // 保存遥测设置
  const saveTelemetrySettings = async (values: any) => {
    try {
      setSaving(true);
      // 在实际应用中，这里应该调用API保存设置
      console.log('保存遥测设置:', values);
      
      // 模拟API请求
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      message.success('遥测设置保存成功');
      
      // 更新本地状态
      if (settings) {
        setSettings({
          ...settings,
          telemetry: values,
        });
      }
    } catch (err: any) {
      message.error(err.message || '保存设置失败');
    } finally {
      setSaving(false);
    }
  };

  // 保存安全设置
  const saveSecuritySettings = async (values: any) => {
    try {
      setSaving(true);
      // 在实际应用中，这里应该调用API保存设置
      console.log('保存安全设置:', values);
      
      // 模拟API请求
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      message.success('安全设置保存成功');
      
      // 更新本地状态
      if (settings) {
        setSettings({
          ...settings,
          security: values,
        });
      }
    } catch (err: any) {
      message.error(err.message || '保存设置失败');
    } finally {
      setSaving(false);
    }
  };

  // 保存通知设置
  const saveNotificationSettings = async (values: any) => {
    try {
      setSaving(true);
      // 在实际应用中，这里应该调用API保存设置
      console.log('保存通知设置:', values);
      
      // 模拟API请求
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      message.success('通知设置保存成功');
      
      // 更新本地状态
      if (settings) {
        setSettings({
          ...settings,
          notification: values,
        });
      }
    } catch (err: any) {
      message.error(err.message || '保存设置失败');
    } finally {
      setSaving(false);
    }
  };

  // 测试邮件设置
  const testEmailSettings = async () => {
    try {
      const values = notificationForm.getFieldsValue();
      
      message.info('正在发送测试邮件...');
      
      // 在实际应用中，这里应该调用API测试邮件设置
      // 模拟API请求
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      message.success('测试邮件发送成功！请检查您的收件箱。');
    } catch (err: any) {
      message.error(err.message || '测试邮件发送失败');
    }
  };

  return (
    <div>
      <Title level={2}>系统设置</Title>
      
      {loading ? (
        <div style={{ textAlign: 'center', padding: '50px' }}>
          <Spin size="large" />
        </div>
      ) : error ? (
        <div style={{ marginBottom: 16 }}>
          <Alert
            message="错误"
            description={error}
            type="error"
            showIcon
          />
        </div>
      ) : (
        <Card>
          <Tabs 
            activeKey={activeTab} 
            onChange={setActiveTab}
            type="card"
          >
            {/* 通用设置 */}
            <TabPane tab="通用设置" key="general">
              <Form
                form={generalForm}
                layout="vertical"
                onFinish={saveGeneralSettings}
                initialValues={settings?.general}
              >
                <Form.Item
                  name="siteName"
                  label="站点名称"
                  rules={[{ required: true, message: '请输入站点名称' }]}
                >
                  <Input placeholder="站点名称" />
                </Form.Item>
                
                <Form.Item
                  name="siteDescription"
                  label="站点描述"
                >
                  <Input.TextArea rows={3} placeholder="站点描述" />
                </Form.Item>
                
                <Form.Item
                  name="adminEmail"
                  label="管理员邮箱"
                  rules={[
                    { required: true, message: '请输入管理员邮箱' },
                    { type: 'email', message: '请输入有效的邮箱地址' }
                  ]}
                >
                  <Input placeholder="管理员邮箱" />
                </Form.Item>
                
                <Form.Item
                  name="dataRetentionDays"
                  label="数据保留天数"
                  rules={[{ required: true, message: '请输入数据保留天数' }]}
                >
                  <InputNumber min={1} max={3650} style={{ width: '100%' }} />
                </Form.Item>
                
                <Form.Item
                  name="enableRegistration"
                  label="启用用户注册"
                  valuePropName="checked"
                >
                  <Switch />
                </Form.Item>
                
                <Divider />
                
                <Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    icon={<SaveOutlined />}
                    loading={saving}
                  >
                    保存设置
                  </Button>
                </Form.Item>
              </Form>
            </TabPane>
            
            {/* 遥测设置 */}
            <TabPane tab="遥测设置" key="telemetry">
              <Form
                form={telemetryForm}
                layout="vertical"
                onFinish={saveTelemetrySettings}
                initialValues={settings?.telemetry}
              >
                <Form.Item
                  name="enableTelemetry"
                  label="启用遥测功能"
                  valuePropName="checked"
                >
                  <Switch />
                </Form.Item>
                
                <Form.Item
                  name="collectAnonymousData"
                  label="收集匿名使用数据"
                  valuePropName="checked"
                >
                  <Switch />
                </Form.Item>
                
                <Form.Item
                  name="sendErrorReports"
                  label="发送错误报告"
                  valuePropName="checked"
                >
                  <Switch />
                </Form.Item>
                
                <Form.Item
                  name="dataRetentionPeriod"
                  label="遥测数据保留天数"
                  rules={[{ required: true, message: '请输入数据保留天数' }]}
                >
                  <InputNumber min={1} max={3650} style={{ width: '100%' }} />
                </Form.Item>
                
                <Divider />
                
                <Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    icon={<SaveOutlined />}
                    loading={saving}
                  >
                    保存设置
                  </Button>
                </Form.Item>
              </Form>
            </TabPane>
            
            {/* 安全设置 */}
            <TabPane tab="安全设置" key="security">
              <Form
                form={securityForm}
                layout="vertical"
                onFinish={saveSecuritySettings}
                initialValues={settings?.security}
              >
                <Form.Item
                  name="sessionTimeoutMinutes"
                  label="会话超时时间（分钟）"
                  rules={[{ required: true, message: '请输入会话超时时间' }]}
                >
                  <InputNumber min={5} max={1440} style={{ width: '100%' }} />
                </Form.Item>
                
                <Form.Item
                  name="maxLoginAttempts"
                  label="最大登录尝试次数"
                  rules={[{ required: true, message: '请输入最大登录尝试次数' }]}
                >
                  <InputNumber min={1} max={10} style={{ width: '100%' }} />
                </Form.Item>
                
                <Form.Item
                  name="passwordMinLength"
                  label="密码最小长度"
                  rules={[{ required: true, message: '请输入密码最小长度' }]}
                >
                  <InputNumber min={6} max={32} style={{ width: '100%' }} />
                </Form.Item>
                
                <Form.Item
                  name="requireStrongPasswords"
                  label="要求强密码"
                  valuePropName="checked"
                  tooltip="强密码需要包含大小写字母、数字和特殊字符"
                >
                  <Switch />
                </Form.Item>
                
                <Form.Item
                  name="jwtExpirationMinutes"
                  label="JWT 令牌过期时间（分钟）"
                  rules={[{ required: true, message: '请输入JWT令牌过期时间' }]}
                >
                  <InputNumber min={5} max={1440} style={{ width: '100%' }} />
                </Form.Item>
                
                <Form.Item
                  name="refreshTokenExpirationDays"
                  label="刷新令牌过期时间（天）"
                  rules={[{ required: true, message: '请输入刷新令牌过期时间' }]}
                >
                  <InputNumber min={1} max={90} style={{ width: '100%' }} />
                </Form.Item>
                
                <Divider />
                
                <Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    icon={<SaveOutlined />}
                    loading={saving}
                  >
                    保存设置
                  </Button>
                </Form.Item>
              </Form>
            </TabPane>
            
            {/* 通知设置 */}
            <TabPane tab="通知设置" key="notification">
              <Form
                form={notificationForm}
                layout="vertical"
                onFinish={saveNotificationSettings}
                initialValues={settings?.notification}
              >
                <Form.Item
                  name="enableEmailNotifications"
                  label="启用邮件通知"
                  valuePropName="checked"
                >
                  <Switch />
                </Form.Item>
                
                <Divider orientation="left">SMTP 设置</Divider>
                
                <Form.Item
                  name="smtpServer"
                  label="SMTP 服务器"
                  rules={[{ required: true, message: '请输入SMTP服务器' }]}
                >
                  <Input placeholder="例如: smtp.example.com" />
                </Form.Item>
                
                <Form.Item
                  name="smtpPort"
                  label="SMTP 端口"
                  rules={[{ required: true, message: '请输入SMTP端口' }]}
                >
                  <InputNumber min={1} max={65535} style={{ width: '100%' }} />
                </Form.Item>
                
                <Form.Item
                  name="smtpUsername"
                  label="SMTP 用户名"
                >
                  <Input placeholder="SMTP 用户名" />
                </Form.Item>
                
                <Form.Item
                  name="smtpPassword"
                  label="SMTP 密码"
                >
                  <Input.Password placeholder="SMTP 密码" />
                </Form.Item>
                
                <Form.Item
                  name="smtpUseSsl"
                  label="使用 SSL/TLS"
                  valuePropName="checked"
                >
                  <Switch />
                </Form.Item>
                
                <Form.Item
                  name="emailFrom"
                  label="发件人"
                  rules={[{ required: true, message: '请输入发件人' }]}
                >
                  <Input placeholder="例如: CangJie <noreply@example.com>" />
                </Form.Item>
                
                <Divider />
                
                <Form.Item>
                  <Space>
                    <Button
                      type="primary"
                      htmlType="submit"
                      icon={<SaveOutlined />}
                      loading={saving}
                    >
                      保存设置
                    </Button>
                    <Button
                      onClick={testEmailSettings}
                    >
                      发送测试邮件
                    </Button>
                  </Space>
                </Form.Item>
              </Form>
            </TabPane>
          </Tabs>
        </Card>
      )}
    </div>
  );
};

export default Settings; 