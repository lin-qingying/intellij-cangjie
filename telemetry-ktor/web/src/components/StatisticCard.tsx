import React from 'react';
import { Card, Typography } from 'antd';
import { ArrowRightOutlined, RiseOutlined, FallOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import CountUp from 'react-countup';

const { Text } = Typography;

interface StatisticCardProps {
  title: string;
  value: number;
  icon: React.ReactNode;
  color: 'primary' | 'success' | 'warning' | 'danger';
  link: string;
  subtitle?: string;
  trend?: 'up' | 'down' | 'stable';
}

const colorMap = {
  primary: 'stat-card-primary',
  success: 'stat-card-success',
  warning: 'stat-card-warning',
  danger: 'stat-card-danger'
};

const StatisticCard: React.FC<StatisticCardProps> = ({
  title,
  value,
  icon,
  color,
  link,
  subtitle,
  trend
}) => {
  return (
    <Card 
      style={{ 
        borderRadius: '1rem',
        overflow: 'hidden',
        transition: 'all 0.3s',
        height: '100%',
        position: 'relative',
        border: 'none',
        boxShadow: '0 4px 12px rgba(0,0,0,0.05)'
      }}
      bodyStyle={{ padding: '1.5rem' }}
      className={`dashboard-stat-card ${colorMap[color]}`}
    >
      <div style={{ position: 'relative', zIndex: 2 }}>
        <Text style={{ 
          textTransform: 'uppercase',
          letterSpacing: '1px',
          fontSize: '0.85rem',
          fontWeight: 600,
          marginBottom: '0.5rem',
          opacity: 0.8,
          color: 'white'
        }}>
          {title}
        </Text>
        <div style={{ fontSize: '2.5rem', fontWeight: 700, color: 'white' }}>
          <CountUp end={value} separator="," duration={1.5} />
        </div>
        {subtitle && (
          <div style={{ fontSize: '0.9rem', opacity: 0.8, color: 'white', marginTop: '0.5rem' }}>
            {trend === 'up' && <RiseOutlined style={{ marginRight: 5, color: '#52c41a' }} />}
            {trend === 'down' && <FallOutlined style={{ marginRight: 5, color: '#ff4d4f' }} />}
            {trend === 'stable' && <ArrowRightOutlined style={{ marginRight: 5 }} />}
            {subtitle}
          </div>
        )}
      </div>
      <div style={{ 
        position: 'absolute',
        right: '-15px',
        bottom: '-15px',
        opacity: 0.15,
        fontSize: '7rem',
        transform: 'rotate(-15deg)',
        color: 'white'
      }}>
        {icon}
      </div>
      <div style={{ 
        borderTop: '1px solid rgba(255, 255, 255, 0.2)',
        padding: '0.75rem 0 0',
        marginTop: '1.5rem',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center'
      }}>
        <Link to={link} style={{ color: 'white', textDecoration: 'none' }}>
          查看详情
        </Link>
        <ArrowRightOutlined style={{ color: 'white' }} />
      </div>
    </Card>
  );
};

export default StatisticCard; 