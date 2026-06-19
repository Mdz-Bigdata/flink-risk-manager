import React, { useState } from 'react';
import { ProTable, ModalForm, ProFormText, ProFormSelect, ProFormTextArea, ProFormSwitch } from '@ant-design/pro-components';
import { Button, Space, Popconfirm, message, Divider, Typography, Switch } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, CopyOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import api from '@/utils/request';
import { PageHeader, StatusTag } from '@/components';
import { RISK_LEVEL_CONFIG, ACTION_CONFIG } from '@/constants';
import type { Rule, PageResult } from '@/types';

const { Text } = Typography;

const REFERENCE_CONFIG: Record<string, Partial<Rule> & { remark?: string }> = {
  LOGIN: {
    ruleName: '登录频次异常检测',
    ruleType: 'LOGIN',
    riskLevel: 'HIGH',
    action: 'BLOCK',
    description: '同一用户短时间内多次登录失败，判定为暴力破解风险',
    ruleExpression: "eventType=='LOGIN' && patternType=='LOGIN_FAILURE' && failCount>=3",
    enabled: true,
    remark: '适用场景：登录接口防护。当同一 userId 在 5 分钟内登录失败超过 3 次时触发。',
  },
  ORDER: {
    ruleName: '大额订单异常检测',
    ruleType: 'ORDER',
    riskLevel: 'MEDIUM',
    action: 'REVIEW',
    description: '单个用户短时间内创建大额订单，判定为刷单风险',
    ruleExpression: "eventType=='ORDER' && patternType=='ORDER_FREQUENT' && orderCount>=3 && totalAmount>10000",
    enabled: true,
    remark: '适用场景：电商平台订单风控。单笔金额 > 10000 且 1 小时内下单超过 3 笔时触发。',
  },
  ACTIVITY: {
    ruleName: '活动薅羊毛检测',
    ruleType: 'ACTIVITY',
    riskLevel: 'HIGH',
    action: 'BLOCK',
    description: '同一设备参与活动次数异常，判定为薅羊毛风险',
    ruleExpression: "eventType=='ACTIVITY' && patternType=='ACTIVITY_FREQUENT' && participationCount>=4",
    enabled: true,
    remark: '适用场景：营销活动防刷。同一用户在 24 小时内参与活动超过 4 次时触发。',
  },
};

const RuleList: React.FC = () => {
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRule, setEditingRule] = useState<Rule | null>(null);
  const [isReference, setIsReference] = useState(false);
  const actionRef = React.useRef<ActionType>();

  const columns: ProColumns<Rule>[] = [
    { title: 'ID', dataIndex: 'id', width: 70, search: false },
    { title: '规则名称', dataIndex: 'ruleName', width: 200, ellipsis: true },
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
    {
      title: '执行动作',
      dataIndex: 'action',
      width: 100,
      valueType: 'select',
      valueEnum: Object.fromEntries(
        Object.entries(ACTION_CONFIG).map(([k, v]) => [k, { text: v.label, status: v.status }]),
      ),
      render: (_, record) => <StatusTag type="action" value={record.action || ''} />,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 80,
      search: false,
      render: (_, record) => (
        <Switch
          checked={record.enabled}
          checkedChildren="启用"
          unCheckedChildren="停用"
          onChange={async (checked) => {
            await api.put(`/rules/${record.id}/toggle`);
            message.success(checked ? '已启用' : '已停用');
            actionRef.current?.reload();
          }}
        />
      ),
    },
    { title: '描述', dataIndex: 'description', search: false, ellipsis: true },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      width: 170,
      search: false,
      sorter: true,
    },
    {
      title: '操作',
      width: 150,
      search: false,
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />}
            onClick={() => { setEditingRule(record); setIsReference(false); setModalOpen(true); }}>
            编辑
          </Button>
          <Popconfirm title="确认删除？" onConfirm={async () => {
            await api.delete(`/rules/${record.id}`);
            message.success('删除成功');
            actionRef.current?.reload();
          }}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  const openReference = (type: keyof typeof REFERENCE_CONFIG) => {
    const ref = REFERENCE_CONFIG[type];
    setEditingRule({ ...ref } as Rule);
    setIsReference(true);
    setModalOpen(true);
  };

  return (
    <div>
      <PageHeader title="规则管理" />

      <ProTable<Rule>
        actionRef={actionRef}
        columns={columns}
        rowKey="id"
        pagination={{ pageSize: 10 }}
        search={{ labelWidth: 'auto' }}
        request={async (params) => {
          const res = await api.get<PageResult<Rule>>('/rules', {
            params: { page: params.current, pageSize: params.pageSize, name: params.ruleName, riskLevel: params.riskLevel },
          });
          return { data: (res as unknown as PageResult<Rule>).data || [], success: true, total: (res as unknown as PageResult<Rule>).total || 0 };
        }}
        toolBarRender={() => [
          <Button key="ref-login" icon={<CopyOutlined />} onClick={() => openReference('LOGIN')}>参考：登录</Button>,
          <Button key="ref-order" icon={<CopyOutlined />} onClick={() => openReference('ORDER')}>参考：订单</Button>,
          <Button key="ref-activity" icon={<CopyOutlined />} onClick={() => openReference('ACTIVITY')}>参考：活动</Button>,
          <Button key="add" type="primary" icon={<PlusOutlined />}
            onClick={() => { setEditingRule(null); setIsReference(false); setModalOpen(true); }}>
            新建规则
          </Button>,
        ]}
      />

      <ModalForm
        title={isReference ? '参考配置（可修改后保存）' : editingRule?.id ? '编辑规则' : '新建规则'}
        open={modalOpen}
        onOpenChange={(open) => { setModalOpen(open); if (!open) setIsReference(false); }}
        initialValues={editingRule || {}}
        onFinish={async (values) => {
          if (editingRule?.id && !isReference) {
            await api.put(`/rules/${editingRule.id}`, values);
          } else {
            await api.post('/rules', values);
          }
          message.success('保存成功');
          setModalOpen(false);
          setIsReference(false);
          actionRef.current?.reload();
        }}
        width={640}
        modalProps={{ destroyOnClose: true }}
      >
        {isReference && (
          <>
            <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
              {REFERENCE_CONFIG[editingRule?.ruleType as keyof typeof REFERENCE_CONFIG]?.remark}
            </Text>
            <Divider style={{ margin: '8px 0 16px' }} />
          </>
        )}
        <ProFormText name="ruleName" label="规则名称" rules={[{ required: true }]} />
        <ProFormSelect name="riskLevel" label="风险等级" rules={[{ required: true }]}
          options={Object.entries(RISK_LEVEL_CONFIG).map(([k, v]) => ({ label: v.label, value: k }))} />
        <ProFormSelect name="action" label="执行动作" rules={[{ required: true }]}
          options={Object.entries(ACTION_CONFIG).map(([k, v]) => ({ label: v.label, value: k }))} />
        <ProFormSwitch name="enabled" label="是否启用" />
        <ProFormTextArea name="description" label="描述" />
        <ProFormTextArea name="ruleExpression" label="规则表达式 (Aviator)"
          placeholder="例：eventType=='LOGIN' && patternType=='LOGIN_FAILURE' && failCount>=3" />
      </ModalForm>
    </div>
  );
};

export default RuleList;
