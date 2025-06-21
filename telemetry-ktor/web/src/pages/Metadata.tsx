import React, { useState, useEffect } from 'react';
import { 
  Table, Card, Typography, Spin, Pagination, 
  Tag, Space, Button, Modal, Tooltip, Row, Col,
  Input, Select, Form, Divider, Alert, message
} from 'antd';
import { 
  InfoCircleOutlined, ExclamationCircleOutlined, 
  SearchOutlined, FilterOutlined, ReloadOutlined, 
  CloseCircleOutlined, ExportOutlined, QuestionCircleOutlined,
  DesktopOutlined, DatabaseOutlined, ArrowRightOutlined,
  ClockCircleOutlined
} from '@ant-design/icons';
import { getMetadata } from '../services/api';
import type { ApiResponse, MetadataData, TelemetryMetadata } from '../types';
import { Link } from 'react-router-dom';

const { Title, Text, Paragraph } = Typography;
const { Option } = Select;
const { confirm } = Modal;

interface MetadataSearchParams {
  page: number;
  pageSize: number;
  os?: string;
  ideVersion?: string;
  pluginVersion?: string;
}

const Metadata: React.FC = () => {
  const [loading, setLoading] = useState<boolean>(true);
  const [metadataData, setMetadataData] = useState<MetadataData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [searchParams, setSearchParams] = useState<MetadataSearchParams>({
    page: 1,
    pageSize: 10
  });
  const [detailModalVisible, setDetailModalVisible] = useState<boolean>(false);
  const [currentMetadata, setCurrentMetadata] = useState<TelemetryMetadata | null>(null);
  const [searchText, setSearchText] = useState<string>('');
  const [filterVisible, setFilterVisible] = useState<boolean>(true);
  const [helpModalVisible, setHelpModalVisible] = useState<boolean>(false);
  const [ideVersions, setIdeVersions] = useState<string[]>([]);
  const [pluginVersions, setPluginVersions] = useState<string[]>([]);
  const [osList, setOsList] = useState<string[]>(['Windows', 'Mac OS X', 'Linux']);

  // 获取元数据列表
  const fetchMetadata = async (params: MetadataSearchParams) => {
    try {
      setLoading(true);
      const response = await getMetadata(params);
      const result = response.data as ApiResponse<MetadataData>;
      
      if (result.success && result.data) {
        setMetadataData(result.data);
        
        // 提取IDE版本和插件版本列表
        if (result.data.metadata && result.data.metadata.length > 0) {
          const ideVersionSet = new Set<string>();
          const pluginVersionSet = new Set<string>();
          const osSet = new Set<string>();
          
          result.data.metadata.forEach(item => {
            if (item.ideVersion) ideVersionSet.add(item.ideVersion);
            if (item.pluginVersion) pluginVersionSet.add(item.pluginVersion);
            if (item.os) osSet.add(item.os);
          });
          
          setIdeVersions(Array.from(ideVersionSet));
          setPluginVersions(Array.from(pluginVersionSet));
          setOsList(Array.from(osSet));
        }
        
        setError(null);
      } else {
        setError(result.message || '获取元数据失败');
      }
    } catch (err: any) {
      setError(err.message || '获取元数据失败');
    } finally {
      setLoading(false);
    }
  };

  // 初始加载
  useEffect(() => {
    fetchMetadata(searchParams);
  }, []);

  // 处理筛选条件变更
  const handleFilterChange = (changedValues: Partial<MetadataSearchParams>) => {
    const newParams = { ...searchParams, ...changedValues, page: 1 };
    setSearchParams(newParams);
    fetchMetadata(newParams);
  };

  // 处理分页变化
  const handlePageChange = (page: number, pageSize?: number) => {
    const newParams = { ...searchParams, page, pageSize: pageSize || searchParams.pageSize };
    setSearchParams(newParams);
    fetchMetadata(newParams);
  };

  // 重置筛选条件
  const resetFilters = () => {
    const defaultParams = {
      page: 1,
      pageSize: 10
    };
    setSearchParams(defaultParams);
    fetchMetadata(defaultParams);
    setSearchText('');
  };

  // 查看元数据详情
  const viewDetails = (metadata: TelemetryMetadata) => {
    setCurrentMetadata(metadata);
    setDetailModalVisible(true);
  };

  // 获取操作系统标签颜色
  const getOsBadgeColor = (os: string) => {
    if (os.includes('Windows')) return 'blue';
    if (os.includes('Mac')) return 'cyan';
    if (os.includes('Linux')) return 'green';
    return 'default';
  };

  // 表格列配置
  const columns = [
    {
      title: '系统ID',
      dataIndex: 'systemId',
      key: 'systemId',
      width: 150,
      ellipsis: true,
      render: (text: string) => (
        <Tooltip title={text}>
          <span>{text}</span>
        </Tooltip>
      )
    },
    {
      title: '插件版本',
      dataIndex: 'pluginVersion',
      key: 'pluginVersion',
      width: 100,
      render: (text: string) => <Tag color="blue">{text}</Tag>
    },
    {
      title: 'IDE版本',
      dataIndex: 'ideVersion',
      key: 'ideVersion',
      width: 100,
      render: (text: string, record: TelemetryMetadata) => (
        <>
          {text} <Text type="secondary" style={{ fontSize: '12px' }}>({record.ideBuild})</Text>
        </>
      )
    },
    {
      title: '操作系统',
      key: 'os',
      width: 120,
      render: (record: TelemetryMetadata) => (
        <>
          <Tag color={getOsBadgeColor(record.os)}>{record.os}</Tag>
          <Text type="secondary" style={{ fontSize: '12px' }}>{record.osVersion}</Text>
        </>
      )
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
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (record: TelemetryMetadata) => (
        <Space>
          <Button 
            type="default" 
            size="small"
            icon={<InfoCircleOutlined />} 
            onClick={() => viewDetails(record)}
          >
            快速查看
          </Button>
          <Link to={`/metadata/${record._id}`}>
            <Button 
              type="primary" 
              size="small"
              icon={<ArrowRightOutlined />}
            >
              详情
            </Button>
          </Link>
        </Space>
      )
    }
  ];

  return (
    <div>
      <Title level={2}>元数据列表</Title>
      
      {/* 筛选卡片 */}
      <Card 
        title={
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <FilterOutlined style={{ marginRight: 8 }} />
            <span>筛选条件</span>
          </div>
        }
        extra={
          <Button 
            type="text" 
            icon={filterVisible ? <CloseCircleOutlined /> : <FilterOutlined />} 
            onClick={() => setFilterVisible(!filterVisible)}
          />
        }
        style={{ marginBottom: 16 }}
      >
        {filterVisible && (
          <Form layout="vertical" onFinish={() => {}}>
            <Row gutter={16}>
              <Col xs={24} md={24} lg={24}>
                <Input
                  placeholder="搜索元数据..."
                  prefix={<SearchOutlined />}
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                  style={{ marginBottom: 16 }}
                  suffix={
                    searchText ? (
                      <CloseCircleOutlined onClick={() => setSearchText('')} />
                    ) : null
                  }
                />
                <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
                  搜索将在当前页面结果中进行筛选
                </Text>
              </Col>
              
              <Col xs={24} md={8}>
                <Form.Item label="操作系统">
                  <Select
                    placeholder="选择操作系统"
                    value={searchParams.os}
                    onChange={(value) => handleFilterChange({ os: value })}
                    allowClear
                    style={{ width: '100%' }}
                  >
                    {osList.map((os) => (
                      <Option key={os} value={os}>
                        {os}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              
              <Col xs={24} md={8}>
                <Form.Item label="IDE版本">
                  <Select
                    placeholder="选择IDE版本"
                    value={searchParams.ideVersion}
                    onChange={(value) => handleFilterChange({ ideVersion: value })}
                    allowClear
                    style={{ width: '100%' }}
                  >
                    {ideVersions.map((version) => (
                      <Option key={version} value={version}>
                        {version}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              
              <Col xs={24} md={8}>
                <Form.Item label="插件版本">
                  <Select
                    placeholder="选择插件版本"
                    value={searchParams.pluginVersion}
                    onChange={(value) => handleFilterChange({ pluginVersion: value })}
                    allowClear
                    style={{ width: '100%' }}
                  >
                    {pluginVersions.map((version) => (
                      <Option key={version} value={version}>
                        {version}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              
              <Col xs={24} md={8}>
                <Form.Item label="每页显示">
                  <Select
                    value={searchParams.pageSize}
                    onChange={(value) => handleFilterChange({ pageSize: value })}
                    style={{ width: '100%' }}
                  >
                    <Option value={10}>10 条/页</Option>
                    <Option value={25}>25 条/页</Option>
                    <Option value={50}>50 条/页</Option>
                    <Option value={100}>100 条/页</Option>
                  </Select>
                </Form.Item>
              </Col>
              
              <Col xs={24} md={16}>
                <div style={{ display: 'flex', alignItems: 'flex-end', height: '100%', marginBottom: 24 }}>
                  <Space>
                    <Button
                      type="primary"
                      icon={<FilterOutlined />}
                      onClick={() => fetchMetadata(searchParams)}
                    >
                      应用筛选
                    </Button>
                    <Button
                      icon={<ReloadOutlined />}
                      onClick={resetFilters}
                    >
                      重置筛选
                    </Button>
                  </Space>
                  
                  {metadataData?.totalCount !== undefined && (
                    <Text style={{ marginLeft: 'auto' }}>
                      共 <Text strong>{metadataData.totalCount}</Text> 条记录
                    </Text>
                  )}
                </div>
              </Col>
            </Row>
          </Form>
        )}
      </Card>
      
      {/* 元数据列表卡片 */}
      <Card
        title={
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <DatabaseOutlined style={{ marginRight: 8 }} />
            <span>元数据列表</span>
          </div>
        }
        extra={
          <Space>
            <Button 
              icon={<ExportOutlined />}
              onClick={() => message.info('导出功能开发中')}
            >
              导出
            </Button>
            <Button
              icon={<QuestionCircleOutlined />}
              onClick={() => setHelpModalVisible(true)}
            />
          </Space>
        }
      >
        {loading ? (
          <div style={{ textAlign: 'center', padding: '50px' }}>
            <Spin size="large" />
          </div>
        ) : error ? (
          <div style={{ textAlign: 'center', padding: '20px' }}>
            <Alert
              message="错误"
              description={error}
              type="error"
              showIcon
            />
          </div>
        ) : (
          <>
            <Table
              rowKey="_id"
              columns={columns}
              dataSource={metadataData?.metadata || []}
              pagination={false}
            />
            
            {metadataData?.totalPages && metadataData.totalPages > 1 && (
              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16 }}>
                <Pagination
                  current={searchParams.page}
                  pageSize={searchParams.pageSize}
                  total={metadataData.totalCount}
                  showQuickJumper
                  showSizeChanger={false}
                  onChange={handlePageChange}
                  showTotal={(total) => `共 ${total} 条记录`}
                />
              </div>
            )}
          </>
        )}
      </Card>

      {/* 元数据详情模态框 */}
      <Modal
        title={
          <div>
            <InfoCircleOutlined style={{ marginRight: 8, color: '#1890ff' }} />
            元数据详情
          </div>
        }
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>
        ]}
        width={600}
      >
        {currentMetadata && (
          <>
            <div style={{ marginBottom: 16 }}>
              <Tag color="blue">ID: {currentMetadata._id}</Tag>
            </div>
            
            <Row gutter={[16, 16]}>
              <Col span={12}>
                <div className="metadata-detail-item" style={{ marginBottom: '8px' }}>
                  <Text type="secondary">系统ID:</Text> 
                  <div style={{ marginTop: '4px' }}>
                    <Text ellipsis={{ tooltip: currentMetadata.systemId }}>
                      {currentMetadata.systemId}
                    </Text>
                  </div>
                </div>
              </Col>
              <Col span={12}>
                <div className="metadata-detail-item" style={{ marginBottom: '8px' }}>
                  <Text type="secondary">插件版本:</Text> 
                  <div style={{ marginTop: '4px' }}>
                    <Tag color="blue">{currentMetadata.pluginVersion}</Tag>
                  </div>
                </div>
              </Col>
              <Col span={12}>
                <div className="metadata-detail-item" style={{ marginBottom: '8px' }}>
                  <Text type="secondary">IDE版本:</Text> 
                  <div style={{ marginTop: '4px' }}>{currentMetadata.ideVersion}</div>
                </div>
              </Col>
              <Col span={12}>
                <div className="metadata-detail-item" style={{ marginBottom: '8px' }}>
                  <Text type="secondary">IDE构建号:</Text> 
                  <div style={{ marginTop: '4px' }}>{currentMetadata.ideBuild}</div>
                </div>
              </Col>
              <Col span={12}>
                <div className="metadata-detail-item" style={{ marginBottom: '8px' }}>
                  <Text type="secondary">操作系统:</Text> 
                  <div style={{ marginTop: '4px' }}>{currentMetadata.os}</div>
                </div>
              </Col>
              <Col span={12}>
                <div className="metadata-detail-item" style={{ marginBottom: '8px' }}>
                  <Text type="secondary">系统版本:</Text> 
                  <div style={{ marginTop: '4px' }}>{currentMetadata.osVersion}</div>
                </div>
              </Col>
              <Col span={12}>
                <div className="metadata-detail-item" style={{ marginBottom: '8px' }}>
                  <Text type="secondary">Java版本:</Text> 
                  <div style={{ marginTop: '4px' }}>{currentMetadata.javaVersion}</div>
                </div>
              </Col>
              <Col span={12}>
                <div className="metadata-detail-item" style={{ marginBottom: '8px' }}>
                  <Text type="secondary">时间:</Text> 
                  <div style={{ marginTop: '4px' }}>{currentMetadata.formattedTime}</div>
                </div>
              </Col>
            </Row>
            
            <Divider />
            
            <div style={{ textAlign: 'center' }}>
              <Link to={`/events?metadataId=${currentMetadata._id}`}>
                <Button type="primary" icon={<ArrowRightOutlined />}>
                  查看相关事件
                </Button>
              </Link>
            </div>
          </>
        )}
      </Modal>
      
      {/* 帮助模态框 */}
      <Modal
        title={
          <div>
            <QuestionCircleOutlined style={{ marginRight: 8, color: '#1890ff' }} />
            帮助信息
          </div>
        }
        open={helpModalVisible}
        onCancel={() => setHelpModalVisible(false)}
        footer={[
          <Button key="ok" type="primary" onClick={() => setHelpModalVisible(false)}>
            了解了
          </Button>
        ]}
      >
        <Title level={5}>如何使用筛选功能</Title>
        <ul>
          <li>使用<Text strong>操作系统</Text>筛选特定系统的元数据</li>
          <li>使用<Text strong>IDE版本</Text>筛选特定IDE版本的元数据</li>
          <li>使用<Text strong>插件版本</Text>筛选特定插件版本的元数据</li>
          <li>使用<Text strong>搜索框</Text>在当前页面结果中快速查找</li>
        </ul>
        
        <Title level={5}>操作系统标识</Title>
        <Space>
          <Tag color="blue">Windows</Tag>
          <Tag color="cyan">Mac OS X</Tag>
          <Tag color="green">Linux</Tag>
          <Tag color="default">其他</Tag>
        </Space>
        
        <Divider />
        
        <Title level={5}>导出数据</Title>
        <Paragraph>
          您可以将当前筛选结果导出为CSV或JSON格式，方便进行进一步分析。
        </Paragraph>
      </Modal>
    </div>
  );
};

export default Metadata; 