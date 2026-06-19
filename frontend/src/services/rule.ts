import axios from 'axios';

const BASE_URL = '/api';

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

export interface PageResult<T> {
  data: T[];
  total: number;
  page: number;
  pageSize: number;
}

export async function getRules(params: {
  page?: number;
  pageSize?: number;
  name?: string;
  riskLevel?: string;
  action?: string;
  enabled?: boolean;
}) {
  const res = await axios.get<PageResult<Rule>>(`${BASE_URL}/rules`, { params });
  return res.data;
}

export async function getEnabledRules() {
  const res = await axios.get<Rule[]>(`${BASE_URL}/rules/enabled`);
  return res.data;
}

export async function getRule(id: number) {
  const res = await axios.get<Rule>(`${BASE_URL}/rules/${id}`);
  return res.data;
}

export async function createRule(data: Partial<Rule>) {
  const res = await axios.post<Rule>(`${BASE_URL}/rules`, data);
  return res.data;
}

export async function updateRule(id: number, data: Partial<Rule>) {
  const res = await axios.put<Rule>(`${BASE_URL}/rules/${id}`, data);
  return res.data;
}

export async function deleteRule(id: number) {
  const res = await axios.delete(`${BASE_URL}/rules/${id}`);
  return res.data;
}

export async function toggleRule(id: number) {
  const res = await axios.put<Rule>(`${BASE_URL}/rules/${id}/toggle`);
  return res.data;
}

export async function batchEnable(ids: number[]) {
  const res = await axios.put(`${BASE_URL}/rules/batch/enable`, ids);
  return res.data;
}

export async function batchDisable(ids: number[]) {
  const res = await axios.put(`${BASE_URL}/rules/batch/disable`, ids);
  return res.data;
}
