import React from 'react';
import { ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import api from '@/utils/request';
import { PageHeader, StatusTag } from '@/components';
import { RISK_LEVEL_CONFIG, ACTION_CONFIG, EVENT_TYPE_CONFIG } from '@/constants';
import type { RiskRecord, PageResult } from '@/types';

const RiskList: React.FC = () => {
  const columns: ProColumns<RiskRecord>[] = [
    { title: 'ID', dataIndex: 'id', width: 70, search: false },
    { title: '用户ID', dataIndex: 'userId', width: 130 },
    {
      title: '事件类型',
      dataIndex: 'eventType',
      width: 110,
      valueType: 'select',
      valueEnum: Object.fromEntries(
        Object.entries(EVENT_TYPE_CONFIG).map(([k, v]) => [k, { text: v.label, status: v.status }]),
      ),
      render: (_, record) => <StatusTag type="eventType" value={record.eventType} />,
    },
    {
      title: '风险等级',
      dataIndex: 'riskLevel',
      width: 100,
      valueType: 'select',
      valueEnum: Object.fromEntries(
        Object.entries(RISK_LEVEL_CONFIG).map(([k, v]) => [k, { text: v.label, status: v.status }]),
      ),
      render: (_, record) => <StatusTag type="riskLevel" value={record.riskLevel} />,
    },
    { title: '风险分数', dataIndex: 'riskScore', width: 90, search: false },
    {
      title: '执行动作',
      dataIndex: 'action',
      width: 100,
      valueType: 'select',
      valueEnum: Object.fromEntries(
        Object.entries(ACTION_CONFIG).map(([k, v]) => [k, { text: v.label, status: v.status }]),
      ),
      render: (_, record) => <StatusTag type="action" value={record.action} />,
    },
    {
      title: '检测时间',
      dataIndex: 'createTime',
      width: 170,
      search: false,
      sorter: true,
    },
  ];

  return (
    <div>
      <PageHeader title="风险记录" />

      <ProTable<RiskRecord>
        columns={columns}
        rowKey="id"
        pagination={{ pageSize: 10 }}
        search={{ labelWidth: 'auto' }}
        request={async (params) => {
          const res = await api.get<PageResult<RiskRecord>>('/risks', {
            params: {
              page: params.current,
              pageSize: params.pageSize,
              userId: params.userId,
              eventType: params.eventType,
              riskLevel: params.riskLevel,
              action: params.action,
            },
          });
          return {
            data: (res as unknown as PageResult<RiskRecord>).data || [],
            success: true,
            total: (res as unknown as PageResult<RiskRecord>).total || 0,
          };
        }}
      />
    </div>
  );
};

export default RiskList;
