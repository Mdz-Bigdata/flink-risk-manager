import React from 'react';
import { Row, Col, Tag, Divider, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import {
  SettingOutlined,
  SafetyCertificateOutlined,
  AlertOutlined,
  CheckCircleOutlined,
  FileProtectOutlined,
  WarningOutlined,
  ExperimentOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import { StatCard, PageHeader } from '@/components';
import { useOverview } from '@/hooks/useStats';
import { RISK_LEVEL_CONFIG, RULE_TYPE_CONFIG } from '@/constants';

const { Title } = Typography;

const Home: React.FC = () => {
  const { data: stats, loading } = useOverview();
  const navigate = useNavigate();

  return (
    <div>
      <PageHeader title="系统概览" />

      {/* ========== 总体统计 ========== */}
      <Title level={5} style={{ marginTop: 8, marginBottom: 16, color: '#1e293b', fontWeight: 600 }}>
        <CheckCircleOutlined style={{ marginRight: 8, color: '#2563eb' }} />
        总体统计
      </Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="规则总数"
            value={stats?.totalRules ?? 0}
            icon={<SettingOutlined />}
            onClick={() => navigate('/rules')}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="启用规则"
            value={stats?.enabledRules ?? 0}
            icon={<FileProtectOutlined />}
            onClick={() => navigate('/rules')}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="今日风险事件"
            value={stats?.todayRisks ?? 0}
            icon={<SafetyCertificateOutlined />}
            onClick={() => navigate('/risks')}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="高风险命中"
            value={stats?.todayHighRisks ?? 0}
            icon={<AlertOutlined />}
            valueStyle={{ color: (stats?.todayHighRisks ?? 0) > 0 ? '#ff4d4f' : undefined }}
            onClick={() => navigate('/risks')}
          />
        </Col>
      </Row>

      <Divider style={{ margin: '32px 0' }} />

      {/* ========== 规则维度 ========== */}
      <Title level={5} style={{ marginBottom: 16, color: '#1e293b', fontWeight: 600 }}>
        <ExperimentOutlined style={{ marginRight: 8, color: '#2563eb' }} />
        规则维度
      </Title>
      <Row gutter={[16, 16]}>
        {Object.entries(RULE_TYPE_CONFIG).map(([key, { label, color }]) => (
          <Col xs={24} sm={12} lg={8} key={key}>
            <StatCard
              title={label}
              value={stats?.ruleTypeDistribution?.[key] ?? 0}
              icon={<Tag color={color}>{label}</Tag>}
              onClick={() => navigate('/rules')}
            />
          </Col>
        ))}
      </Row>

      <Divider style={{ margin: '32px 0' }} />

      {/* ========== 风险维度 ========== */}
      <Title level={5} style={{ marginBottom: 16, color: '#1e293b', fontWeight: 600 }}>
        <WarningOutlined style={{ marginRight: 8, color: '#f59e0b' }} />
        风险维度
      </Title>
      <Row gutter={[16, 16]}>
        {Object.entries(RISK_LEVEL_CONFIG).map(([key, { label, color }]) => (
          <Col xs={24} sm={12} lg={6} key={key}>
            <StatCard
              title={label}
              value={stats?.riskLevelDistribution?.[key] ?? 0}
              valueStyle={{ color }}
              onClick={() => navigate('/risks')}
            />
          </Col>
        ))}
      </Row>
    </div>
  );
};

export default Home;
