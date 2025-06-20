import React, { useState, useEffect } from 'react';
import { 
  Table, Card, Form, Input, Select, DatePicker, Button, Space, 
  Tag, Tooltip, Modal, message, Typography, Pagination, Row, Col, Spin 
} from 'antd';
import { 
  SearchOutlined, 
  ReloadOutlined, 
  DeleteOutlined, 
  InfoCircleOutlined,
  ExclamationCircleOutlined 
} from '@ant-design/icons';
import { getEvents, deleteEvent, bulkDeleteEvents } from '../services/api';
import type { ApiResponse, EventsData, TelemetryEvent } from '../types';
import dayjs from 'dayjs';

const { Title } = Typography;
const { Option } = Select;
const { confirm } = Modal;

interface EventsSearchParams {
  page: number;
  pageSize: number;
  category?: string;
  name?: string;
  startDate?: string;
  endDate?: string;
  groupBy?: string;
}

const Events: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState<boolean>(true);
  const [eventsData, setEventsData] = useState<EventsData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [searchParams, setSearchParams] = useState<EventsSearchParams>({
    page: 1,
    pageSize: 10
  });
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [propertyModalVisible, setPropertyModalVisible] = useState<boolean>(false);
  const [currentEvent, setCurrentEvent] = useState<TelemetryEvent | null>(null);

  // 获取事件列表数据
  const fetchEvents = async (params: EventsSearchParams) => {
    try {
      setLoading(true);
      const response = await getEvents(params);
      const result = response.data as ApiResponse<EventsData>;
      
      if (result.success && result.data) {
        setEventsData(result.data);
        setError(null);
      } else {
        setError(result.message || '获取事件数据失败');
      }
    } catch (err: any) {
      setError(err.message || '获取事件数据失败');
    } finally {
      setLoading(false);
    }
  };

  // 初始加载
  useEffect(() => {
    fetchEvents(searchParams);
  }, []);

  // 处理搜索
  const handleSearch = (values: any) => {
    const params: EventsSearchParams = {
      ...searchParams,
      page: 1, // 搜索时重置为第一页
      category: values.category,
      name: values.name,
      startDate: values.startDate ? dayjs(values.startDate).format('YYYY-MM-DD') : undefined,
      endDate: values.endDate ? dayjs(values.endDate).format('YYYY-MM-DD') : undefined,
      groupBy: values.groupBy
    };
    setSearchParams(params);
    fetchEvents(params);
  };

  // 重置搜索
  const resetSearch = () => {
    form.resetFields();
    const params: EventsSearchParams = {
      page: 1,
      pageSize: searchParams.pageSize
    };
    setSearchParams(params);
    fetchEvents(params);
  };

  // 处理分页变化
  const handlePageChange = (page: number, pageSize?: number) => {
    const newParams = { ...searchParams, page, pageSize: pageSize || searchParams.pageSize };
    setSearchParams(newParams);
    fetchEvents(newParams);
  };

  // 查看事件属性
  const viewProperties = (event: TelemetryEvent) => {
    setCurrentEvent(event);
    setPropertyModalVisible(true);
  };

  // 删除单个事件
  const handleDeleteEvent = (id: string) => {
    confirm({
      title: '确定要删除这条事件记录吗？',
      icon: <ExclamationCircleOutlined />,
      content: '删除后无法恢复',
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const response = await deleteEvent(id);
          const result = response.data as ApiResponse<any>;
          
          if (result.success) {
            message.success('事件删除成功');
            fetchEvents(searchParams);
          } else {
            message.error(result.message || '删除事件失败');
          }
        } catch (err: any) {
          message.error(err.message || '删除事件失败');
        }
      }
    });
  };

  // 批量删除事件
  const handleBulkDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要删除的事件');
      return;
    }

    confirm({
      title: `确定要删除选中的 ${selectedRowKeys.length} 条事件记录吗？`,
      icon: <ExclamationCircleOutlined />,
      content: '删除后无法恢复',
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const response = await bulkDeleteEvents(selectedRowKeys as string[]);
          const result = response.data as ApiResponse<any>;
          
          if (result.success) {
            message.success(`成功删除 ${result.data?.deletedCount || 0} 条事件`);
            setSelectedRowKeys([]);
            fetchEvents(searchParams);
          } else {
            message.error(result.message || '批量删除事件失败');
          }
        } catch (err: any) {
          message.error(err.message || '批量删除事件失败');
        }
      }
    });
  };

  // 表格列配置
  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 150,
      ellipsis: true,
    },
    {
      title: '类别',
      dataIndex: 'category',
      key: 'category',
      width: 100,
      render: (text: string) => {
        let color = 'blue';
        switch (text) {
          case 'error': color = 'red'; break;
          case 'warning': color = 'orange'; break;
          case 'info': color = 'green'; break;
          case 'success': color = 'green'; break;
        }
        return <Tag color={color}>{text}</Tag>;
      }
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      width: 150,
    },
    {
      title: '值',
      dataIndex: 'value',
      key: 'value',
      width: 150,
      ellipsis: true,
    },
    {
      title: '时间',
      key: 'timestamp',
      width: 180,
      render: (record: TelemetryEvent) => (
        <span>
          {eventsData?.formattedTimestamps[record.id] || record.timestamp}
        </span>
      )
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (record: TelemetryEvent) => (
        <Space>
          <Button 
            type="text" 
            icon={<InfoCircleOutlined />} 
            onClick={() => viewProperties(record)}
          >
            属性
          </Button>
          <Button 
            type="text" 
            danger 
            icon={<DeleteOutlined />} 
            onClick={() => handleDeleteEvent(record.id)}
          >
            删除
          </Button>
        </Space>
      )
    }
  ];

  return (
    <div>
      <Title level={2}>事件列表</Title>
      
      <Card className="filter-form">
        <Form
          form={form}
          layout="horizontal"
          onFinish={handleSearch}
          initialValues={{
            category: undefined,
            name: undefined,
            startDate: undefined,
            endDate: undefined,
            groupBy: undefined
          }}
        >
          <Row gutter={16}>
            <Col span={6}>
              <Form.Item name="category" label="事件类别">
                <Select
                  allowClear
                  placeholder="选择事件类别"
                >
                  {eventsData?.categories.map(category => (
                    <Option key={category} value={category}>{category}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="name" label="事件名称">
                <Select
                  allowClear
                  placeholder="选择事件名称"
                >
                  {eventsData?.eventNames.map(name => (
                    <Option key={name} value={name}>{name}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="startDate" label="开始日期">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="endDate" label="结束日期">
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          
          <Row gutter={16}>
            <Col span={6}>
              <Form.Item name="groupBy" label="分组方式">
                <Select
                  allowClear
                  placeholder="选择分组方式"
                >
                  <Option value="value">按值分组</Option>
                  <Option value="category">按类别分组</Option>
                  <Option value="name">按名称分组</Option>
                </Select>
              </Form.Item>
            </Col>
            <Col span={18} style={{ textAlign: 'right' }}>
              <Space>
                <Button type="primary" icon={<SearchOutlined />} htmlType="submit">
                  搜索
                </Button>
                <Button icon={<ReloadOutlined />} onClick={resetSearch}>
                  重置
                </Button>
                <Button 
                  danger 
                  icon={<DeleteOutlined />} 
                  disabled={selectedRowKeys.length === 0}
                  onClick={handleBulkDelete}
                >
                  批量删除 ({selectedRowKeys.length})
                </Button>
              </Space>
            </Col>
          </Row>
        </Form>
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
            rowKey="id"
            columns={columns}
            dataSource={eventsData?.events || []}
            pagination={false}
            rowSelection={{
              selectedRowKeys,
              onChange: (keys) => setSelectedRowKeys(keys)
            }}
          />
          
          {eventsData && eventsData.totalPages > 0 && (
            <div style={{ marginTop: 16, textAlign: 'right' }}>
              <Pagination
                current={eventsData.currentPage}
                pageSize={eventsData.pageSize}
                total={eventsData.totalCount}
                showSizeChanger
                showQuickJumper
                showTotal={(total) => `共 ${total} 条记录`}
                onChange={handlePageChange}
              />
            </div>
          )}
        </Card>
      )}

      {/* 事件属性详情模态框 */}
      <Modal
        title="事件属性详情"
        open={propertyModalVisible}
        onCancel={() => setPropertyModalVisible(false)}
        footer={null}
        width={600}
      >
        {currentEvent && (
          <div>
            <p><strong>ID:</strong> {currentEvent.id}</p>
            <p><strong>类别:</strong> <Tag>{currentEvent.category}</Tag></p>
            <p><strong>名称:</strong> {currentEvent.name}</p>
            <p><strong>值:</strong> {currentEvent.value}</p>
            <p><strong>时间:</strong> {eventsData?.formattedTimestamps[currentEvent.id] || currentEvent.timestamp}</p>
            
            {currentEvent.metadataId && eventsData?.metadataMap[currentEvent.metadataId] && (
              <div>
                <h4>元数据信息</h4>
                <div style={{ background: '#f5f5f5', padding: '10px', borderRadius: '4px' }}>
                  <p><strong>插件版本:</strong> {eventsData.metadataMap[currentEvent.metadataId].pluginVersion}</p>
                  <p><strong>IDE版本:</strong> {eventsData.metadataMap[currentEvent.metadataId].ideVersion}</p>
                  <p><strong>操作系统:</strong> {eventsData.metadataMap[currentEvent.metadataId].os} {eventsData.metadataMap[currentEvent.metadataId].osVersion}</p>
                  <p><strong>Java版本:</strong> {eventsData.metadataMap[currentEvent.metadataId].javaVersion}</p>
                  <p><strong>系统ID:</strong> {eventsData.metadataMap[currentEvent.metadataId].systemId}</p>
                </div>
              </div>
            )}
            
            <h4>事件属性</h4>
            {Object.keys(currentEvent.properties).length > 0 ? (
              <div>
                {Object.entries(currentEvent.properties).map(([key, value]) => (
                  <div key={key} style={{ marginBottom: '5px' }}>
                    <Tooltip title={value}>
                      <Tag color="blue" className="property-tag">{key}: {value}</Tag>
                    </Tooltip>
                  </div>
                ))}
              </div>
            ) : (
              <p>没有属性数据</p>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
};

export default Events; 