import React from 'react';
import { Tag } from 'antd';
import { RISK_LEVEL_CONFIG, ACTION_CONFIG, EVENT_TYPE_CONFIG } from '@/constants';

interface StatusTagProps {
  type: 'riskLevel' | 'action' | 'eventType';
  value: string;
}

const configMap = {
  riskLevel: RISK_LEVEL_CONFIG,
  action: ACTION_CONFIG,
  eventType: EVENT_TYPE_CONFIG,
};

const StatusTag: React.FC<StatusTagProps> = ({ type, value }) => {
  const config = configMap[type]?.[value];
  if (!config) return <Tag>{value}</Tag>;
  return <Tag color={config.color}>{config.label}</Tag>;
};

export default StatusTag;
