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
    { title: <Link to="/"><HomeOutlined /></Link> },
    ...(breadcrumb || [{ title: breadcrumbNameMap[location.pathname] || '' }]),
  ];

  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
      <Space direction="vertical" size={4}>
        <Breadcrumb items={items} />
        {title && <Title level={4} style={{ margin: 0 }}>{title}</Title>}
      </Space>
      {extra}
    </div>
  );
};

export default PageHeader;
