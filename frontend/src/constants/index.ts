/** 风险等级枚举 */
export enum RiskLevel {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

/** 执行动作枚举 */
export enum RiskAction {
  ALLOW = 'ALLOW',
  VERIFY = 'VERIFY',
  REVIEW = 'REVIEW',
  LIMIT = 'LIMIT',
  BLOCK = 'BLOCK',
  DISABLE = 'DISABLE',
}

/** 事件类型枚举 */
export enum EventType {
  LOGIN = 'LOGIN',
  ORDER = 'ORDER',
  ACTIVITY = 'ACTIVITY',
}

/** 风险等级配置映射 */
export const RISK_LEVEL_CONFIG: Record<string, { label: string; color: string; status: string }> = {
  LOW: { label: '低', color: '#52c41a', status: 'Success' },
  MEDIUM: { label: '中', color: '#faad14', status: 'Warning' },
  HIGH: { label: '高', color: '#ff4d4f', status: 'Error' },
  CRITICAL: { label: '严重', color: '#cf1322', status: 'Error' },
};

/** 执行动作配置映射 */
export const ACTION_CONFIG: Record<string, { label: string; color: string; status: string }> = {
  ALLOW: { label: '放行', color: '#52c41a', status: 'Success' },
  VERIFY: { label: '验证', color: '#faad14', status: 'Warning' },
  REVIEW: { label: '审核', color: '#1677ff', status: 'Processing' },
  LIMIT: { label: '限制', color: '#faad14', status: 'Warning' },
  BLOCK: { label: '阻断', color: '#ff4d4f', status: 'Error' },
  DISABLE: { label: '禁用', color: '#ff4d4f', status: 'Error' },
};

/** 事件类型配置映射 */
export const EVENT_TYPE_CONFIG: Record<string, { label: string; color: string; status: string }> = {
  LOGIN: { label: '登录', color: '#1677ff', status: 'Processing' },
  ORDER: { label: '订单', color: '#52c41a', status: 'Success' },
  ACTIVITY: { label: '活动', color: '#faad14', status: 'Warning' },
};

/** 规则类型配置映射 */
export const RULE_TYPE_CONFIG: Record<string, { label: string; color: string }> = {
  LOGIN: { label: '登录检测', color: '#1677ff' },
  ORDER: { label: '订单检测', color: '#52c41a' },
  ACTIVITY: { label: '活动检测', color: '#faad14' },
  OTHER: { label: '其他', color: '#8c8c8c' },
};
