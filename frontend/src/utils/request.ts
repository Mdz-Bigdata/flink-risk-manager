import { useState, useEffect, useCallback, useRef } from 'react';
import type { AxiosRequestConfig } from 'axios';
import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
});

// 响应拦截器
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const msg = error.response?.data?.message || error.message || '请求失败';
    console.error('[API Error]', msg);
    return Promise.reject(error);
  },
);

export default api;

/** 通用请求 hook */
export function useRequest<T>(
  requestFn: () => Promise<T>,
  options?: { immediate?: boolean; onSuccess?: (data: T) => void; onError?: (err: unknown) => void },
) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const mountedRef = useRef(true);

  const run = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await requestFn();
      if (mountedRef.current) {
        setData(result);
        options?.onSuccess?.(result);
      }
    } catch (err) {
      if (mountedRef.current) {
        setError(err);
        options?.onError?.(err);
      }
    } finally {
      if (mountedRef.current) setLoading(false);
    }
  }, [requestFn, options]);

  useEffect(() => {
    mountedRef.current = true;
    if (options?.immediate !== false) run();
    return () => { mountedRef.current = false; };
  }, [run, options?.immediate]);

  return { data, loading, error, run, setData };
}

/** 通用分页请求 hook */
export function usePageRequest<T, P extends Record<string, unknown>>(
  requestFn: (params: P & { page: number; pageSize: number }) => Promise<{ data: T[]; total: number }>,
) {
  const [data, setData] = useState<T[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [params, setParams] = useState<P>({} as P);

  const fetch = useCallback(async (page: number, pageSize: number, extra?: Partial<P>) => {
    setLoading(true);
    try {
      const merged = { ...params, ...extra, page, pageSize } as P & { page: number; pageSize: number };
      const res = await requestFn(merged);
      setData(res.data || []);
      setTotal(res.total || 0);
    } catch (err) {
      console.error('[PageRequest Error]', err);
    } finally {
      setLoading(false);
    }
  }, [requestFn, params]);

  return { data, total, loading, fetch, setParams };
}
