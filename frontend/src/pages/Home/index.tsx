import React from 'react';
import { Row, Col, Tag, Spin } from 'antd';
import {
  SettingOutlined,
  SafetyCertificateOutlined,
  AlertOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import { StatCard, PageHeader } from '@/components';
import { useOverview } from '@/hooks/useStats';
import { RISK_LEVEL_CONFIG, RULE_TYPE_CONFIG } from '@/constants';

const Home: React.FC = () => {
  const { data: stats, loading } = useOverview();

  return (
    <div>
      <PageHeader title="系统概览" />

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="启用规则"
            value={stats?.enabledRules ?? 0}
            icon={<SettingOutlined />}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="今日风险事件"
            value={stats?.todayRisks ?? 0}
            icon={<SafetyCertificateOutlined />}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="高风险命中"
            value={stats?.todayHighRisks ?? 0}
            icon={<AlertOutlined />}
            valueStyle={{ color: (stats?.todayHighRisks ?? 0) > 0 ? '#ff4d4f' : undefined }}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard
            title="规则总数"
            value={stats?.totalRules ?? 0}
            icon={<CheckCircleOutlined />}
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        {Object.entries(RULE_TYPE_CONFIG).map(([key, { label, color }]) => (
          <Col xs={24} sm={12} lg={6} key={key}>
            <StatCard
              title={label}
              value={stats?.ruleTypeDistribution?.[key] ?? 0}
              icon={<Tag color={color}>{label}</Tag>}
            />
          </Col>
        ))}
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        {Object.entries(RISK_LEVEL_CONFIG).map(([key, { label, color }]) => (
          <Col xs={24} sm={12} lg={6} key={key}>
            <StatCard
              title={label}
              value={stats?.riskLevelDistribution?.[key] ?? 0}
              valueStyle={{ color }}
            />
          </Col>
        ))}
      </Row>
    </div>
  );
};

export default Home;
