import React from 'react';
import { Breadcrumb, Space, Typography } from 'antd';
import { useLocation, Link } from 'react-router-dom';
import { HomeOutlined } from '@ant-design/icons';

const { Title } = Typography;

interface PageHeaderProps {
  title?: string;
  extra?: React.ReactNode;
  breadcrumb?: Array<{ title: string; path?: string }>;
}

const breadcrumbNameMap: Record<string, string> = {
  '/': '首页',
  '/rules': '规则管理',
  '/risks': '风险记录',
  '/dashboard': '数据看板',
};

const PageHeader: React.FC<PageHeaderProps> = ({ title, extra, breadcrumb }) => {
  const location = useLocation();

  const items = [
    { title: <Link to="/"><HomeOutlined style={{ color: '#94a3b8' }} /></Link> },
    ...(breadcrumb || [{ title: <span style={{ color: '#64748b' }}>{breadcrumbNameMap[location.pathname] || ''}</span> }]),
  ];

  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 28 }}>
      <Space direction="vertical" size={6}>
        <Breadcrumb
          items={items}
          separator={<span style={{ color: '#cbd5e1' }}>/</span>}
        />
        {title && (
          <Title level={3} style={{
            margin: 0,
            fontFamily: "'Outfit', sans-serif",
            fontWeight: 700,
            fontSize: 24,
            color: '#1e293b',
            letterSpacing: '-0.5px',
          }}>
            {title}
          </Title>
        )}
      </Space>
      {extra}
    </div>
  );
};

export default PageHeader;
