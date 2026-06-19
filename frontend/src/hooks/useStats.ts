import api, { useRequest } from '@/utils/request';
import type { OverviewStats, TrendData, TopRuleData } from '@/types';

export function useOverview() {
  return useRequest<OverviewStats>(() => api.get('/stats/overview'));
}

export function useTrend() {
  return useRequest<TrendData>(() => api.get('/stats/trend'));
}

export function useTopRules() {
  return useRequest<TopRuleData>(() => api.get('/stats/top-rules'));
}
