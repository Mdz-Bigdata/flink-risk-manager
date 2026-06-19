import axios from 'axios';

const BASE_URL = '/api';

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

export async function getOverview() {
  const res = await axios.get<OverviewStats>(`${BASE_URL}/stats/overview`);
  return res.data;
}

export async function getTrend() {
  const res = await axios.get<TrendData>(`${BASE_URL}/stats/trend`);
  return res.data;
}

export async function getTopRules() {
  const res = await axios.get<TopRuleData>(`${BASE_URL}/stats/top-rules`);
  return res.data;
}
