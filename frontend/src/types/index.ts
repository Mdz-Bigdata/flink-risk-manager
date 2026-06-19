export interface Rule {
  id?: number;
  ruleName: string;
  ruleType?: string;
  riskLevel: string;
  action?: string;
  ruleExpression: string;
  description?: string;
  enabled?: boolean;
  createTime?: string;
  updateTime?: string;
}

export interface RiskRecord {
  id: number;
  userId: string;
  eventId: string;
  eventType: string;
  riskLevel: string;
  riskScore: number;
  action: string;
  details: string;
  createTime: string;
}

export interface PageResult<T> {
  data: T[];
  total: number;
  page: number;
  pageSize: number;
}

export interface OverviewStats {
  totalRules: number;
  enabledRules: number;
  todayRisks: number;
  todayHighRisks: number;
  ruleTypeDistribution: Record<string, number>;
  riskLevelDistribution: Record<string, number>;
  eventTypeDistribution: Record<string, number>;
}

export interface TrendData {
  dailyData: Array<{
    date: string;
    total: number;
    high: number;
  }>;
}

export interface TopRuleData {
  topRules: Array<{
    ruleName: string;
    hitCount: number;
  }>;
}
