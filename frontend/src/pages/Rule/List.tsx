import React, { useState } from 'react';
import { ProTable, ModalForm, ProFormText, ProFormSelect, ProFormTextArea, ProFormSwitch } from '@ant-design/pro-components';
import { Button, Space, Popconfirm, message, Divider, Typography, Switch, Card, Alert, Tag, Table } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, CopyOutlined, QuestionCircleOutlined, BookOutlined } from '@ant-design/icons';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import api from '@/utils/request';
import { PageHeader, StatusTag } from '@/components';
import { RISK_LEVEL_CONFIG, ACTION_CONFIG } from '@/constants';
import type { Rule, PageResult } from '@/types';

const { Text, Title, Paragraph } = Typography;

// ========== 表达式文档 ==========
const EXPRESSION_DOC = {
  variables: [
    { name: 'eventType', type: 'String', desc: '事件类型：LOGIN / ORDER / ACTIVITY', example: "eventType=='LOGIN'" },
    { name: 'patternType', type: 'String', desc: 'CEP 匹配模式类型', example: "patternType=='LOGIN_FAILURE'" },
    { name: 'userId', type: 'String', desc: '用户 ID', example: "userId=='user_001'" },
    { name: 'failCount', type: 'int', desc: '登录失败次数（登录场景）', example: 'failCount>=3' },
    { name: 'amount', type: 'double', desc: '订单金额（下单场景）', example: 'amount>10000' },
    { name: 'orderCount', type: 'int', desc: '下单次数（下单场景）', example: 'orderCount>=3' },
    { name: 'participationCount', type: 'int', desc: '活动参与次数（活动场景）', example: 'participationCount>=4' },
    { name: 'actionType', type: 'String', desc: '活动行为类型：PARTICIPATE / CLAIM_COUPON', example: "actionType=='CLAIM_COUPON'" },
    { name: 'channel', type: 'String', desc: '渠道：app / web / mini_program / bot', example: "channel=='bot'" },
    { name: 'success', type: 'boolean', desc: '登录是否成功', example: 'success==false' },
  ],
  operators: [
    { syntax: '==', desc: '等于', example: "eventType=='LOGIN'" },
    { syntax: '!=', desc: '不等于', example: "eventType!='LOGIN'" },
    { syntax: '>', desc: '大于', example: 'failCount>3' },
    { syntax: '>=', desc: '大于等于', example: 'failCount>=3' },
    { syntax: '<', desc: '小于', example: 'amount<100' },
    { syntax: '<=', desc: '小于等于', example: 'amount<=100' },
    { syntax: '&&', desc: '逻辑与', example: "eventType=='LOGIN' && failCount>=3" },
    { syntax: '||', desc: '逻辑或', example: "riskLevel=='HIGH' || riskLevel=='CRITICAL'" },
    { syntax: '=~', desc: '正则匹配', example: "productId=~'ABNORMAL.*'" },
  ],
  patterns: [
    { name: 'LOGIN_FAILURE', desc: '连续登录失败', trigger: '同一用户 5s 内连续 3 次 success=false' },
    { name: 'LOCATION_CHANGE', desc: '异地登录', trigger: '同一用户 5s 内连续 2 次 success=true（IP变化）' },
    { name: 'ORDER_FREQUENT', desc: '频繁下单', trigger: '同一用户 5s 内连续 3 次下单' },
    { name: 'ORDER_RAPID', desc: '快速下单', trigger: '同一用户 5s 内连续 2 次下单' },
    { name: 'ACTIVITY_FREQUENT', desc: '频繁参与活动', trigger: '同一用户 5s 内连续 4 次 actionType=PARTICIPATE' },
    { name: 'COUPON_REPEAT', desc: '重复领券', trigger: '同一用户 5s 内连续 3 次 actionType=CLAIM_COUPON' },
  ],
};

const REFERENCE_CONFIG: Record<string, Partial<Rule> & { remark?: string }> = {
  LOGIN: {
    ruleName: '登录频次异常检测',
    ruleType: 'LOGIN',
    riskLevel: 'HIGH',
    action: 'BLOCK',
    description: '同一用户短时间内多次登录失败，判定为暴力破解风险',
    ruleExpression: "eventType=='LOGIN' && patternType=='LOGIN_FAILURE' && failCount>=3",
    enabled: true,
    remark: '适用场景：登录接口防护。当同一 userId 在 5 秒内登录失败超过 3 次时触发。',
  },
  ORDER: {
    ruleName: '大额订单异常检测',
    ruleType: 'ORDER',
    riskLevel: 'MEDIUM',
    action: 'REVIEW',
    description: '单个用户短时间内创建大额订单，判定为刷单风险',
    ruleExpression: "eventType=='ORDER' && patternType=='ORDER_FREQUENT' && orderCount>=3 && amount>10000",
    enabled: true,
    remark: '适用场景：电商平台订单风控。单笔金额 > 10000 且 5 秒内下单超过 3 笔时触发。',
  },
  ACTIVITY: {
    ruleName: '活动薅羊毛检测',
    ruleType: 'ACTIVITY',
    riskLevel: 'HIGH',
    action: 'BLOCK',
    description: '同一设备参与活动次数异常，判定为薅羊毛风险',
    ruleExpression: "eventType=='ACTIVITY' && patternType=='ACTIVITY_FREQUENT' && participationCount>=4",
    enabled: true,
    remark: '适用场景：营销活动防刷。同一用户在 5 秒内参与活动超过 4 次时触发。',
  },
};

const RuleList: React.FC = () => {
  const [modalOpen, setModalOpen] = useState(false);
  const [docOpen, setDocOpen] = useState(false);
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

      {/* 表达式文档入口 */}
      <Alert
        message="Aviator 表达式语法指南"
        description="编写规则表达式前，请先了解可用的变量、运算符和 CEP 模式类型。"
        type="info"
        showIcon
        icon={<BookOutlined />}
        action={
          <Button size="small" type="primary" ghost icon={<QuestionCircleOutlined />} onClick={() => setDocOpen(true)}>
            查看文档
          </Button>
        }
        style={{ marginBottom: 16 }}
      />

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

      {/* 规则编辑/新建弹窗 */}
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

      {/* 表达式文档弹窗 */}
      <ModalForm
        title="Aviator 表达式语法指南"
        open={docOpen}
        onOpenChange={setDocOpen}
        submitter={false}
        width={800}
        modalProps={{ destroyOnClose: true }}
      >
        <Alert
          message="规则表达式使用 Aviator 脚本引擎，支持 Java 风格的运算符和逻辑表达式。"
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />

        <Title level={5}>可用变量</Title>
        <Table
          dataSource={EXPRESSION_DOC.variables}
          columns={[
            { title: '变量名', dataIndex: 'name', width: 140 },
            { title: '类型', dataIndex: 'type', width: 80 },
            { title: '说明', dataIndex: 'desc' },
            { title: '示例', dataIndex: 'example', render: (v: string) => <Tag color="blue">{v}</Tag> },
          ]}
          pagination={false}
          size="small"
          rowKey="name"
          style={{ marginBottom: 16 }}
        />

        <Title level={5}>运算符</Title>
        <Table
          dataSource={EXPRESSION_DOC.operators}
          columns={[
            { title: '语法', dataIndex: 'syntax', width: 100 },
            { title: '说明', dataIndex: 'desc' },
            { title: '示例', dataIndex: 'example', render: (v: string) => <Tag color="green">{v}</Tag> },
          ]}
          pagination={false}
          size="small"
          rowKey="syntax"
          style={{ marginBottom: 16 }}
        />

        <Title level={5}>CEP 模式类型</Title>
        <Table
          dataSource={EXPRESSION_DOC.patterns}
          columns={[
            { title: '模式名', dataIndex: 'name', width: 160 },
            { title: '说明', dataIndex: 'desc' },
            { title: '触发条件', dataIndex: 'trigger' },
          ]}
          pagination={false}
          size="small"
          rowKey="name"
          style={{ marginBottom: 16 }}
        />

        <Title level={5}>示例规则</Title>
        <Card size="small" style={{ background: '#f6ffed', borderColor: '#b7eb8f' }}>
          <Paragraph>
            <Text strong>登录失败风暴：</Text>
            <br />
            <Tag color="blue">{'eventType==\'LOGIN\' && patternType==\'LOGIN_FAILURE\' && failCount>=3'}</Tag>
          </Paragraph>
          <Paragraph>
            <Text strong>大额订单检测：</Text>
            <br />
            <Tag color="blue">{'eventType==\'ORDER\' && amount>10000'}</Tag>
          </Paragraph>
          <Paragraph>
            <Text strong>活动频繁参与：</Text>
            <br />
            <Tag color="blue">{'eventType==\'ACTIVITY\' && patternType==\'ACTIVITY_FREQUENT\' && participationCount>=4'}</Tag>
          </Paragraph>
        </Card>
      </ModalForm>
    </div>
  );
};

export default RuleList;
