import React, { useState, useEffect } from 'react';
import {
  Table, Card, Button, Space, Typography, Modal, Form,
  Input, Select, message, Popconfirm, Tag, Spin
} from 'antd';
import {
  UserAddOutlined, EditOutlined, DeleteOutlined,
  ExclamationCircleOutlined, UserOutlined, LockOutlined
} from '@ant-design/icons';
import { getUsers, createUser, updateUser, deleteUser } from '../services/api';
import type { ApiResponse } from '../types';

const { Title } = Typography;
const { Option } = Select;
const { confirm } = Modal;

interface User {
  username: string;
  displayName?: string;
  email?: string;
  role: string;
  createdAt?: string;
  lastLogin?: string;
}

interface UsersData {
  users: User[];
  currentPage: number;
  totalPages: number;
  pageSize: number;
  totalCount: number;
}

const Users: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(true);
  const [usersData, setUsersData] = useState<UsersData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [modalVisible, setModalVisible] = useState<boolean>(false);
  const [modalTitle, setModalTitle] = useState<string>('添加用户');
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [form] = Form.useForm();

  // 获取用户列表
  const fetchUsers = async (page: number = 1, pageSize: number = 10) => {
    try {
      setLoading(true);
      const response = await getUsers({ page, pageSize });
      const result = response.data as ApiResponse<UsersData>;
      
      if (result.success && result.data) {
        setUsersData(result.data);
        setError(null);
      } else {
        setError(result.message || '获取用户列表失败');
      }
    } catch (err: any) {
      setError(err.message || '获取用户列表失败');
    } finally {
      setLoading(false);
    }
  };

  // 初始加载
  useEffect(() => {
    fetchUsers();
  }, []);

  // 处理分页变化
  const handlePageChange = (page: number, pageSize?: number) => {
    fetchUsers(page, pageSize);
  };

  // 打开添加用户模态框
  const showAddUserModal = () => {
    setModalTitle('添加用户');
    setEditingUser(null);
    form.resetFields();
    setModalVisible(true);
  };

  // 打开编辑用户模态框
  const showEditUserModal = (user: User) => {
    setModalTitle('编辑用户');
    setEditingUser(user);
    form.setFieldsValue({
      username: user.username,
      displayName: user.displayName,
      email: user.email,
      role: user.role,
      password: '', // 编辑时不填充密码
    });
    setModalVisible(true);
  };

  // 处理模态框确认
  const handleModalOk = () => {
    form.validateFields().then(async (values) => {
      try {
        if (editingUser) {
          // 更新用户
          const response = await updateUser(editingUser.username, values);
          const result = response.data as ApiResponse<any>;
          
          if (result.success) {
            message.success('用户更新成功');
            setModalVisible(false);
            fetchUsers(usersData?.currentPage || 1, usersData?.pageSize || 10);
          } else {
            message.error(result.message || '用户更新失败');
          }
        } else {
          // 创建用户
          const response = await createUser(values);
          const result = response.data as ApiResponse<any>;
          
          if (result.success) {
            message.success('用户创建成功');
            setModalVisible(false);
            fetchUsers(usersData?.currentPage || 1, usersData?.pageSize || 10);
          } else {
            message.error(result.message || '用户创建失败');
          }
        }
      } catch (err: any) {
        message.error(err.message || '操作失败');
      }
    });
  };

  // 处理删除用户
  const handleDeleteUser = (username: string) => {
    confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: `确定要删除用户 "${username}" 吗？此操作不可撤销。`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const response = await deleteUser(username);
          const result = response.data as ApiResponse<any>;
          
          if (result.success) {
            message.success('用户删除成功');
            fetchUsers(usersData?.currentPage || 1, usersData?.pageSize || 10);
          } else {
            message.error(result.message || '用户删除失败');
          }
        } catch (err: any) {
          message.error(err.message || '删除失败');
        }
      }
    });
  };

  // 获取角色标签颜色
  const getRoleTagColor = (role: string) => {
    switch (role.toLowerCase()) {
      case 'admin':
        return 'red';
      case 'user':
        return 'blue';
      default:
        return 'default';
    }
  };

  // 表格列配置
  const columns = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
    },
    {
      title: '显示名',
      dataIndex: 'displayName',
      key: 'displayName',
      render: (text: string) => text || '-',
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      render: (text: string) => text || '-',
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: (role: string) => (
        <Tag color={getRoleTagColor(role)}>{role.toUpperCase()}</Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (text: string) => text || '-',
    },
    {
      title: '最后登录',
      dataIndex: 'lastLogin',
      key: 'lastLogin',
      render: (text: string) => text || '-',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record: User) => (
        <Space>
          <Button
            type="text"
            icon={<EditOutlined />}
            onClick={() => showEditUserModal(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定要删除此用户吗？"
            onConfirm={() => handleDeleteUser(record.username)}
            okText="是"
            cancelText="否"
          >
            <Button
              type="text"
              danger
              icon={<DeleteOutlined />}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Title level={2}>用户管理</Title>
      
      <Card style={{ marginBottom: 16 }}>
        <Button
          type="primary"
          icon={<UserAddOutlined />}
          onClick={showAddUserModal}
        >
          添加用户
        </Button>
      </Card>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '50px' }}>
          <Spin size="large" />
        </div>
      ) : error ? (
        <div style={{ textAlign: 'center', color: 'red', padding: '50px' }}>
          {error}
        </div>
      ) : (
        <Card>
          <Table
            rowKey="username"
            columns={columns}
            dataSource={usersData?.users || []}
            pagination={{
              current: usersData?.currentPage || 1,
              pageSize: usersData?.pageSize || 10,
              total: usersData?.totalCount || 0,
              onChange: handlePageChange,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total) => `共 ${total} 条记录`,
            }}
          />
        </Card>
      )}

      {/* 用户表单模态框 */}
      <Modal
        title={modalTitle}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        okText="确定"
        cancelText="取消"
      >
        <Form
          form={form}
          layout="vertical"
        >
          <Form.Item
            name="username"
            label="用户名"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 3, message: '用户名至少3个字符' }
            ]}
          >
            <Input 
              prefix={<UserOutlined />} 
              placeholder="用户名" 
              disabled={!!editingUser}
            />
          </Form.Item>

          {!editingUser && (
            <Form.Item
              name="password"
              label="密码"
              rules={[
                { required: !editingUser, message: '请输入密码' },
                { min: 6, message: '密码至少6个字符' }
              ]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="密码" />
            </Form.Item>
          )}

          {editingUser && (
            <Form.Item
              name="password"
              label="密码 (留空表示不修改)"
              rules={[
                { min: 6, message: '密码至少6个字符' }
              ]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="新密码 (可选)" />
            </Form.Item>
          )}

          <Form.Item
            name="displayName"
            label="显示名称"
          >
            <Input placeholder="显示名称 (可选)" />
          </Form.Item>

          <Form.Item
            name="email"
            label="邮箱"
            rules={[
              { type: 'email', message: '请输入有效的邮箱地址' }
            ]}
          >
            <Input placeholder="邮箱 (可选)" />
          </Form.Item>

          <Form.Item
            name="role"
            label="角色"
            rules={[
              { required: true, message: '请选择角色' }
            ]}
          >
            <Select placeholder="选择角色">
              <Option value="admin">管理员</Option>
              <Option value="user">普通用户</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Users; 