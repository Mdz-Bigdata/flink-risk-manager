import React, { useState } from 'react';
import { Layout, Menu, Typography } from 'antd';
import { Routes, Route, Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  DashboardOutlined,
  SafetyCertificateOutlined,
  AlertOutlined,
  BarChartOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import Home from './pages/Home';
import RuleList from './pages/Rule/List';
import RiskList from './pages/Risk';
import Dashboard from './pages/Dashboard';
import './App.css';

const { Sider, Content } = Layout;
const { Text } = Typography;

const menuItems = [
  { key: '/', icon: <DashboardOutlined />, label: '系统概览' },
  { key: '/rules', icon: <SafetyCertificateOutlined />, label: '规则管理' },
  { key: '/risks', icon: <AlertOutlined />, label: '风险记录' },
  { key: '/dashboard', icon: <BarChartOutlined />, label: '数据看板' },
];

const AppLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [collapsed, setCollapsed] = useState(false);

  const selectedKey = menuItems.find((item) =>
    item.key === '/' ? location.pathname === '/' : location.pathname.startsWith(item.key),
  )?.key || '/';

  return (
    <Layout className="app-layout">
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        trigger={null}
        width={220}
        className="app-sider"
      >
        <div className="app-logo" onClick={() => navigate('/')}>
          <ThunderboltOutlined className="app-logo-icon" />
          {!collapsed && <Text className="app-logo-text">Flink 风控平台</Text>}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          className="app-menu"
        />
      </Sider>
      <Layout>
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

const App: React.FC = () => {
  return (
    <Routes>
      <Route path="/" element={<AppLayout />}>
        <Route index element={<Home />} />
        <Route path="rules" element={<RuleList />} />
        <Route path="risks" element={<RiskList />} />
        <Route path="dashboard" element={<Dashboard />} />
      </Route>
    </Routes>
  );
};

export default App;
