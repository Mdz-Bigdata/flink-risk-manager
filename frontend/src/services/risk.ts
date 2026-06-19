import axios from 'axios';

const BASE_URL = '/api';

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

export async function getRiskResults(params: {
  page?: number;
  pageSize?: number;
  userId?: string;
  eventType?: string;
  riskLevel?: string;
  action?: string;
}) {
  const res = await axios.get<PageResult<RiskRecord>>(`${BASE_URL}/risks`, { params });
  return res.data;
}

export async function getRiskResult(id: number) {
  const res = await axios.get<RiskRecord>(`${BASE_URL}/risks/${id}`);
  return res.data;
}
