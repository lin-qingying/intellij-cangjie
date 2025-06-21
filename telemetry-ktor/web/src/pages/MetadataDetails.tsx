import React, { useState, useEffect } from 'react';
import {
  Card, Typography, Button, Tag, Descriptions, Space,
  Row, Col, Divider, message, Spin, Alert, Breadcrumb, Statistic,
  Timeline, Avatar, Tooltip, Empty
} from 'antd';
import {
  DatabaseOutlined, DesktopOutlined, ClockCircleOutlined,
  ArrowLeftOutlined, InfoCircleOutlined, AppstoreOutlined,
  CodeOutlined, LaptopOutlined, LinkOutlined, FileTextOutlined,
  RightOutlined, CheckCircleOutlined, CloudOutlined, GlobalOutlined
} from '@ant-design/icons';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { getMetadataById } from '../services/api';
import { API } from '../constants';
import type { ApiResponse, TelemetryMetadata } from '../types';

const { Title, Text, Paragraph } = Typography;

interface MetadataDetailsParams {
  id: string;
}

const MetadataDetails: React.FC = () => {
  const { id } = useParams<keyof MetadataDetailsParams>() as MetadataDetailsParams;
  const navigate = useNavigate();
  const [loading, setLoading] = useState<boolean>(true);
  const [metadata, setMetadata] = useState<TelemetryMetadata | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [formattedTime, setFormattedTime] = useState<string>('');
  const [eventsCount, setEventsCount] = useState<number>(0);

  // 获取元数据详情
  useEffect(() => {
    const fetchMetadataDetails = async () => {
      try {
        setLoading(true);
        
        // 获取元数据详情
        const response = await getMetadataById(id);
        const result = response.data as ApiResponse<{
          metadata: TelemetryMetadata;
          formattedTime: string;
          eventsCount: number;
        }>;
        
        if (result.success && result.data) {
          setMetadata(result.data.metadata);
          setFormattedTime(result.data.formattedTime);
          setEventsCount(result.data.eventsCount);
          setError(null);
        } else {
          setError(result.message || '获取元数据详情失败');
        }
      } catch (err: any) {
        setError(err.message || '获取元数据详情失败');
      } finally {
        setLoading(false);
      }
    };
    
    fetchMetadataDetails();
  }, [id]);

  // 获取操作系统标签颜色
  const getOsBadgeColor = (os?: string) => {
    if (!os) return 'default';
    
    if (os.includes('Windows')) return 'blue';
    if (os.includes('Mac')) return 'cyan';
    if (os.includes('Linux')) return 'green';
    return 'default';
  };

  // 渲染页面标题和基本信息
  const renderPageHeader = () => (
    <div className="page-header-wrapper" style={{ marginBottom: 24 }}>
      <Card
        bordered={false}
        style={{
          borderRadius: '12px',
          overflow: 'hidden',
          boxShadow: '0 4px 12px rgba(0,0,0,0.05)',
          background: 'linear-gradient(135deg, #1890ff 0%, #096dd9 100%)',
        }}
        bodyStyle={{ padding: '24px' }}
      >
        <div style={{ position: 'relative' }}>
          <div 
            style={{ 
              position: 'absolute', 
              right: -20, 
              top: -20, 
              width: 200, 
              height: 200, 
              borderRadius: '50%', 
              background: 'rgba(255, 255, 255, 0.1)',
              zIndex: 0
            }} 
          />
          <div 
            style={{ 
              position: 'absolute', 
              left: 30, 
              bottom: -50, 
              width: 100, 
              height: 100, 
              borderRadius: '50%', 
              background: 'rgba(255, 255, 255, 0.1)',
              zIndex: 0
            }} 
          />
          
          <Row gutter={[16, 16]} align="middle" style={{ position: 'relative', zIndex: 1 }}>
            <Col xs={24} md={16}>
              <div style={{ marginBottom: 16 }}>
                <Breadcrumb 
                  items={[
                    { 
                      title: (
                        <Link to="/metadata" style={{ color: 'rgba(255, 255, 255, 0.85)' }}>
                          元数据列表
                        </Link>
                      )
                    },
                    { 
                      title: <span style={{ color: 'white' }}>元数据详情</span> 
                    }
                  ]} 
                  separator={<RightOutlined style={{ fontSize: 10, color: 'rgba(255, 255, 255, 0.85)' }} />}
                />
              </div>
              
              <div style={{ color: 'white' }}>
                <Title level={2} style={{ color: 'white', margin: 0 }}>
                  <DatabaseOutlined style={{ marginRight: 8 }} />
                  元数据详情
                </Title>
                {metadata && (
                  <Paragraph style={{ color: 'rgba(255, 255, 255, 0.85)', margin: '8px 0 0 0' }}>
                    系统 ID: {metadata.systemId}
                  </Paragraph>
                )}
              </div>
            </Col>
            
            <Col xs={24} md={8} style={{ textAlign: 'right' }}>
              <Space>
                <Tag 
                  icon={<ClockCircleOutlined />} 
                  color="default"
                  style={{ 
                    background: 'rgba(255, 255, 255, 0.2)', 
                    color: 'white',
                    border: 'none',
                    padding: '4px 8px',
                    fontSize: '14px'
                  }}
                >
                  {formattedTime || '未知时间'}
                </Tag>
                <Button
                  icon={<ArrowLeftOutlined />}
                  onClick={() => navigate('/metadata')}
                  style={{ 
                    background: 'rgba(255, 255, 255, 0.2)', 
                    borderColor: 'rgba(255, 255, 255, 0.3)',
                    color: 'white'
                  }}
                >
                  返回列表
                </Button>
              </Space>
            </Col>
          </Row>
          
          {metadata && (
            <Row gutter={[16, 16]} style={{ marginTop: 24, position: 'relative', zIndex: 1 }}>
              <Col xs={24} sm={12} md={6}>
                <Card style={{ 
                  borderRadius: '8px', 
                  background: 'rgba(255, 255, 255, 0.15)',
                  backdropFilter: 'blur(10px)',
                  border: 'none'
                }}>
                  <Statistic
                    title={<span style={{ color: 'rgba(255, 255, 255, 0.85)' }}>插件版本</span>}
                    value={metadata.pluginVersion}
                    valueStyle={{ color: 'white', fontWeight: 'bold' }}
                    prefix={<AppstoreOutlined />}
                  />
                </Card>
              </Col>
              
              <Col xs={24} sm={12} md={6}>
                <Card style={{ 
                  borderRadius: '8px', 
                  background: 'rgba(255, 255, 255, 0.15)',
                  backdropFilter: 'blur(10px)',
                  border: 'none'
                }}>
                  <Statistic
                    title={<span style={{ color: 'rgba(255, 255, 255, 0.85)' }}>IDE版本</span>}
                    value={metadata.ideVersion}
                    valueStyle={{ color: 'white', fontWeight: 'bold' }}
                    prefix={<LaptopOutlined />}
                  />
                </Card>
              </Col>
              
              <Col xs={24} sm={12} md={6}>
                <Card style={{ 
                  borderRadius: '8px', 
                  background: 'rgba(255, 255, 255, 0.15)',
                  backdropFilter: 'blur(10px)',
                  border: 'none'
                }}>
                  <Statistic
                    title={<span style={{ color: 'rgba(255, 255, 255, 0.85)' }}>操作系统</span>}
                    value={metadata.os}
                    valueStyle={{ color: 'white', fontWeight: 'bold' }}
                    prefix={<DesktopOutlined />}
                  />
                </Card>
              </Col>
              
              <Col xs={24} sm={12} md={6}>
                <Card style={{ 
                  borderRadius: '8px', 
                  background: 'rgba(255, 255, 255, 0.15)',
                  backdropFilter: 'blur(10px)',
                  border: 'none'
                }}>
                  <Statistic
                    title={<span style={{ color: 'rgba(255, 255, 255, 0.85)' }}>关联事件</span>}
                    value={eventsCount}
                    valueStyle={{ color: 'white', fontWeight: 'bold' }}
                    prefix={<FileTextOutlined />}
                    suffix="个"
                  />
                </Card>
              </Col>
            </Row>
          )}
        </div>
      </Card>
    </div>
  );

  return (
    <div>
      {renderPageHeader()}
      
      {error && (
        <div style={{ marginBottom: 16 }}>
          <Alert
            message="错误"
            description={error}
            type="error"
            showIcon
          />
        </div>
      )}
      
      {loading ? (
        <div style={{ textAlign: 'center', padding: '50px' }}>
          <Spin size="large" />
        </div>
      ) : metadata ? (
        <Row gutter={[16, 16]}>
          <Col xs={24} md={12}>
            <Card
              title={
                <div style={{ display: 'flex', alignItems: 'center' }}>
                  <AppstoreOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                  插件信息
                </div>
              }
              bordered={false}
              style={{ 
                height: '100%', 
                borderRadius: '12px',
                overflow: 'hidden',
                boxShadow: '0 4px 12px rgba(0,0,0,0.05)'
              }}
            >
              <div style={{ padding: '20px', textAlign: 'center', background: 'rgba(24, 144, 255, 0.05)', borderRadius: '8px' }}>
                <Avatar 
                  icon={<AppstoreOutlined />} 
                  style={{ backgroundColor: '#1890ff', marginBottom: 16 }} 
                  size={64} 
                />
                <div>
                  <Title level={4}>插件版本</Title>
                  <Tag color="blue" style={{ padding: '4px 12px', fontSize: '16px' }}>
                    {metadata.pluginVersion}
                  </Tag>
                </div>
              </div>
            </Card>
          </Col>
          
          <Col xs={24} md={12}>
            <Card
              title={
                <div style={{ display: 'flex', alignItems: 'center' }}>
                  <LaptopOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                  IDE信息
                </div>
              }
              bordered={false}
              style={{ 
                height: '100%', 
                borderRadius: '12px',
                overflow: 'hidden',
                boxShadow: '0 4px 12px rgba(0,0,0,0.05)'
              }}
            >
              <div style={{ padding: '20px', background: 'rgba(24, 144, 255, 0.05)', borderRadius: '8px' }}>
                <Row gutter={[16, 16]} align="middle">
                  <Col xs={24} md={8} style={{ textAlign: 'center' }}>
                    <Avatar 
                      icon={<CodeOutlined />} 
                      style={{ backgroundColor: '#1890ff' }} 
                      size={64} 
                    />
                  </Col>
                  <Col xs={24} md={16}>
                    <Descriptions column={1} bordered={false} size="small" layout="vertical">
                      <Descriptions.Item label={<Text strong>IDE版本</Text>}>
                        <Text style={{ fontSize: '16px' }}>{metadata.ideVersion}</Text>
                      </Descriptions.Item>
                      <Descriptions.Item label={<Text strong>构建号</Text>}>
                        <Text type="secondary">{metadata.ideBuild}</Text>
                      </Descriptions.Item>
                    </Descriptions>
                  </Col>
                </Row>
              </div>
            </Card>
          </Col>
          
          <Col xs={24} md={12}>
            <Card
              title={
                <div style={{ display: 'flex', alignItems: 'center' }}>
                  <DesktopOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                  系统信息
                </div>
              }
              bordered={false}
              style={{ 
                height: '100%', 
                borderRadius: '12px',
                overflow: 'hidden',
                boxShadow: '0 4px 12px rgba(0,0,0,0.05)'
              }}
            >
              <div style={{ padding: '20px', background: 'rgba(24, 144, 255, 0.05)', borderRadius: '8px' }}>
                <Row gutter={[16, 16]}>
                  <Col span={24} style={{ textAlign: 'center', marginBottom: 16 }}>
                    <Avatar 
                      icon={
                        metadata.os?.includes('Windows') ? <WindowsIcon /> :
                        metadata.os?.includes('Mac') ? <AppleIcon /> :
                        metadata.os?.includes('Linux') ? <LinuxIcon /> :
                        <GlobalOutlined />
                      } 
                      style={{ 
                        backgroundColor: getOsBadgeColor(metadata.os) === 'blue' ? '#1890ff' :
                                        getOsBadgeColor(metadata.os) === 'cyan' ? '#13c2c2' :
                                        getOsBadgeColor(metadata.os) === 'green' ? '#52c41a' : 
                                        '#d9d9d9'
                      }} 
                      size={64} 
                    />
                  </Col>
                  <Col span={12}>
                    <div style={{ textAlign: 'center' }}>
                      <Text type="secondary">操作系统</Text>
                      <div>
                        <Tag color={getOsBadgeColor(metadata.os)} style={{ margin: '8px 0', padding: '2px 8px' }}>
                          {metadata.os}
                        </Tag>
                      </div>
                    </div>
                  </Col>
                  <Col span={12}>
                    <div style={{ textAlign: 'center' }}>
                      <Text type="secondary">系统版本</Text>
                      <div style={{ margin: '8px 0' }}>
                        <Text>{metadata.osVersion}</Text>
                      </div>
                    </div>
                  </Col>
                  <Col span={24}>
                    <div style={{ textAlign: 'center', marginTop: 8 }}>
                      <Text type="secondary">Java版本</Text>
                      <div style={{ margin: '8px 0' }}>
                        <Tag color="purple" style={{ padding: '2px 8px' }}>
                          {metadata.javaVersion || '未知'}
                        </Tag>
                      </div>
                    </div>
                  </Col>
                </Row>
              </div>
            </Card>
          </Col>
          
          <Col xs={24} md={12}>
            <Card
              title={
                <div style={{ display: 'flex', alignItems: 'center' }}>
                  <InfoCircleOutlined style={{ marginRight: 8, color: '#1890ff' }} />
                  元数据信息
                </div>
              }
              bordered={false}
              style={{ 
                height: '100%', 
                borderRadius: '12px',
                overflow: 'hidden',
                boxShadow: '0 4px 12px rgba(0,0,0,0.05)'
              }}
              extra={
                <Link to={`/events?metadataId=${metadata._id}`}>
                  <Button type="primary" icon={<FileTextOutlined />}>
                    查看相关事件
                  </Button>
                </Link>
              }
            >
              <Timeline
                items={[
                  {
                    color: 'blue',
                    dot: <DatabaseOutlined style={{ fontSize: '16px' }} />,
                    children: (
                      <div>
                        <Text type="secondary">ID</Text>
                        <div>
                          <Text ellipsis style={{ maxWidth: '100%' }} title={metadata._id}>
                            {metadata._id}
                          </Text>
                        </div>
                      </div>
                    ),
                  },
                  {
                    color: 'blue',
                    dot: <GlobalOutlined style={{ fontSize: '16px' }} />,
                    children: (
                      <div>
                        <Text type="secondary">系统ID</Text>
                        <div>
                          <Text ellipsis style={{ maxWidth: '100%' }} title={metadata.systemId}>
                            {metadata.systemId}
                          </Text>
                        </div>
                      </div>
                    ),
                  },
                  {
                    color: 'green',
                    dot: <ClockCircleOutlined style={{ fontSize: '16px' }} />,
                    children: (
                      <div>
                        <Text type="secondary">时间戳</Text>
                        <div>
                          <Text>{formattedTime}</Text>
                        </div>
                      </div>
                    ),
                  },
                  {
                    color: 'red',
                    dot: <FileTextOutlined style={{ fontSize: '16px' }} />,
                    children: (
                      <div>
                        <Text type="secondary">关联事件</Text>
                        <div>
                          <Text strong>{eventsCount}</Text> 个事件
                        </div>
                      </div>
                    ),
                  },
                ]}
              />
            </Card>
          </Col>
        </Row>
      ) : null}
    </div>
  );
};

// 自定义图标组件
const WindowsIcon = () => (
  <svg viewBox="0 0 24 24" width="1em" height="1em" fill="currentColor">
    <path d="M0 3.449L9.75 2.1v9.451H0m10.949-9.602L24 0v11.4H10.949M0 12.6h9.75v9.451L0 20.699M10.949 12.6H24V24l-12.9-1.801" />
  </svg>
);

const AppleIcon = () => (
  <svg viewBox="0 0 24 24" width="1em" height="1em" fill="currentColor">
    <path d="M12.152 6.896c-.948 0-2.415-1.078-3.96-1.04-2.04.027-3.91 1.183-4.961 3.014-2.117 3.675-.546 9.103 1.519 12.09 1.013 1.454 2.208 3.09 3.792 3.039 1.52-.065 2.09-.987 3.935-.987 1.831 0 2.35.987 3.96.948 1.637-.026 2.676-1.48 3.676-2.948 1.156-1.688 1.636-3.325 1.662-3.415-.039-.013-3.182-1.221-3.22-4.857-.026-3.04 2.48-4.494 2.597-4.559-1.429-2.09-3.623-2.324-4.39-2.376-2-.156-3.675 1.09-4.61 1.09zM15.53 3.83c.843-1.012 1.4-2.427 1.245-3.83-1.207.052-2.662.805-3.532 1.818-.78.896-1.454 2.338-1.273 3.714 1.338.104 2.715-.688 3.559-1.701" />
  </svg>
);

const LinuxIcon = () => (
  <svg viewBox="0 0 24 24" width="1em" height="1em" fill="currentColor">
    <path d="M12.504 0c-.155 0-.315.008-.48.021-4.226.333-3.105 4.807-3.17 6.298-.076 1.092-.3 1.953-1.05 3.02-.885 1.051-2.127 2.75-2.716 4.521-.278.832-.41 1.684-.287 2.489a.424.424 0 00-.11.105c-.26.268-.45.6-.663.839-.199.199-.485.267-.797.4-.313.136-.658.269-.864.68-.09.189-.136.394-.132.602 0 .199.027.4.055.536.058.399.116.728.04.97-.249.68-.28 1.145-.106 1.484.174.334.535.47.94.601.81.2 1.91.135 2.774.6.926.466 1.866.7 2.616.761.312.027.632.014.932-.059.57-.133 1.23-.6 1.657-1.47a31.64 31.64 0 00.078-1.509 1.24 1.24 0 01.226-.027c.332 0 .805.208 1.188.557.297.265.575.532.673.762.43.094.06.185.034.344-.641 1.014-1.038 1.99-.868 2.758.18.586.653.91 1.343 1.061.478.103 1.03.12 1.53.082.994-.072 1.614-.402 2.228-.765.272-.16.514-.324.736-.47.217-.14.41-.265.547-.38.126-.097.2-.169.2-.257l-.015-.145c-.622-.664-.911-1.427-1.073-2.081-.161-.652-.261-1.162-.481-1.598-.12-.262-.326-.5-.576-.78a13.55 13.55 0 00-.877-.86c-.12-.092-.242-.192-.33-.244-.087-.054-.175-.09-.25-.104.03-.024.05-.052.081-.08.103-.088.225-.185.351-.284.145-.117.321-.244.486-.386.34-.299.708-.732.708-1.376 0-.058-.014-.014-.046-.046-.07-.023-.07-.029-.136-.04-.194.068-.023.121-.038.157-.038.127-.02.297.014.347.038.188.097.446.297.683.515.237.217.443.418.45.43.07.266.33.434.63.434h.022c.172-.007.314-.089.414-.203l.075-.095c.324-.365.783-.71 1.295-.71h.057c.217.008.456.098.732.512.215.326.549.871 1.094 1.483.053.074.147.184.283.274.128.086.289.145.466.145.075 0 .133-.006.181-.013a.938.938 0 00.161-.039c.246-.088.464-.236.671-.484.224-.282.35-.644.35-1.043 0-.2-.049-.439-.141-.812a1.23 1.23 0 01-.083-.399c0-.066.014-.136.047-.208.068-.15.226-.323.402-.504.175-.179.35-.361.434-.482.212-.317.297-.652.256-.952-.077-.543-.5-.934-1.123-1.034a1.79 1.79 0 00-.201-.013c-.646 0-1.445.394-1.987.838a1.97 1.97 0 01-1.143.41 1.95 1.95 0 01-1.142-.41C13.123.395 12.324 0 11.677 0c-.068 0-.136.004-.201.013-.623.1-1.046.491-1.124 1.034-.04.3.044.635.257.952.083.122.258.303.433.482.176.181.335.354.403.504.033.072.047.142.047.208 0 .133-.03.275-.083.399-.092.373-.142.612-.142.812 0 .399.127.761.35 1.043.208.248.426.396.671.484a.938.938 0 00.342.052c.177 0 .338-.059.466-.145.136-.09.23-.2.284-.274.544-.612.878-1.157 1.093-1.483.276-.414.515-.504.732-.512h.057c.512 0 .971.345 1.295.71l.075.095c.1.114.242.196.414.203h.022c.3 0 .56-.168.63-.434.007-.012.213-.213.45-.43.237-.218.495-.418.683-.515.05-.024.22-.058.347-.038.036 0 .09.015.157.038-.011.058-.026.124-.04.194-.032.17-.046.303-.046.36 0 .645.368 1.078.708 1.377.165.142.341.269.486.386.126.099.248.196.351.284.031.028.052.056.081.08a.83.83 0 00-.25.104c-.088.052-.21.152-.33.244-.312.276-.605.56-.877.86-.25.28-.456.518-.576.78-.22.436-.32.946-.481 1.598-.162.654-.451 1.417-1.073 2.081l-.015.145c0 .088.074.16.2.257.137.115.33.24.547.38.222.146.464.31.736.47.614.363 1.234.693 2.228.765.5.038 1.052.021 1.53-.082.69-.151 1.164-.475 1.343-1.061.17-.768-.227-1.744-.868-2.758-.026-.16-.01-.25.034-.344.098-.23.376-.497.673-.762.383-.349.856-.557 1.188-.557.078 0 .15.01.226.027a31.64 31.64 0 00.078 1.509c.427.87 1.087 1.337 1.657 1.47.3.073.62.086.932.059.75-.061 1.69-.295 2.616-.761.864-.465 1.964-.4 2.773-.6.405-.13.766-.267.94-.601.175-.339.143-.804-.106-1.484-.075-.242-.018-.571.04-.97.028-.135.055-.337.055-.536.004-.208-.042-.413-.132-.602-.206-.411-.551-.544-.864-.68-.312-.133-.598-.201-.797-.4-.214-.239-.403-.571-.663-.839a.424.424 0 00-.11-.105c.123-.805-.009-1.657-.287-2.489-.589-1.77-1.832-3.47-2.716-4.521-.75-1.067-.974-1.928-1.05-3.02-.065-1.491 1.056-5.965-3.17-6.298-.165-.013-.325-.021-.48-.021z" />
  </svg>
);

export default MetadataDetails; 