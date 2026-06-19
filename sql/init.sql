-- 规则配置表
CREATE TABLE IF NOT EXISTS rule_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    expression TEXT NOT NULL COMMENT 'Aviator 表达式',
    risk_level VARCHAR(20) NOT NULL COMMENT '风险等级: LOW/MEDIUM/HIGH/CRITICAL',
    action VARCHAR(20) NOT NULL COMMENT '动作: ALLOW/VERIFY/BLOCK',
    weight INT DEFAULT 1 COMMENT '规则权重',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    description VARCHAR(500) COMMENT '规则描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控规则配置表';

-- 风险结果表
CREATE TABLE IF NOT EXISTS risk_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL COMMENT '用户ID',
    event_id VARCHAR(50) NOT NULL COMMENT '事件ID',
    event_type VARCHAR(20) NOT NULL COMMENT '事件类型',
    risk_level VARCHAR(20) NOT NULL COMMENT '风险等级',
    risk_score INT COMMENT '风险分数',
    action VARCHAR(20) COMMENT '执行动作',
    details JSON COMMENT '风险详情',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_event_type (event_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险检测结果表';

-- ============================================================
-- 初始化常见风控规则（覆盖登录、下单、活动场景）
-- 表达式变量来自 CEP 匹配后的 context 上下文
-- ============================================================

-- 登录场景规则
INSERT INTO rule_config (id, name, expression, risk_level, action, weight, enabled, description) VALUES
(1, '登录失败次数过多', 'failCount >= 3', 'HIGH', 'BLOCK', 10, 1, '短时间内登录失败超过3次，疑似暴力破解'),
(2, '异地登录检测', 'patternType == "LOCATION_CHANGE"', 'MEDIUM', 'VERIFY', 5, 1, '检测到IP地址发生变化，可能存在账号盗用风险'),
(3, '可疑设备登录', 'success == false && deviceId =~ "abnormal"', 'MEDIUM', 'VERIFY', 3, 1, '使用可疑设备登录且失败'),
(4, '登录频率异常', 'failCount >= 5', 'CRITICAL', 'BLOCK', 10, 1, '登录失败次数过多，直接拦截');

-- 下单场景规则
INSERT INTO rule_config (id, name, expression, risk_level, action, weight, enabled, description) VALUES
(5, '大额订单检测', 'amount > 10000', 'HIGH', 'REVIEW', 8, 1, '单笔订单金额超过1万元，需要人工审核'),
(6, '频繁下单检测', 'patternType == "ORDER_FREQUENT" && orderCount >= 3', 'HIGH', 'BLOCK', 10, 1, '短时间内频繁下单，疑似刷单'),
(7, '快速连续下单', 'patternType == "ORDER_RAPID"', 'CRITICAL', 'BLOCK', 10, 1, '快速连续提交订单，疑似脚本攻击'),
(8, '异常商品下单', 'productId =~ "ABNORMAL" || productName =~ "异常"', 'MEDIUM', 'VERIFY', 5, 1, '下单商品存在异常标记'),
(9, '可疑支付方式', 'paymentMethod =~ "可疑"', 'MEDIUM', 'VERIFY', 4, 1, '使用可疑支付方式');

-- 活动场景规则
INSERT INTO rule_config (id, name, expression, risk_level, action, weight, enabled, description) VALUES
(10, '频繁参与活动', 'patternType == "ACTIVITY_FREQUENT" && participationCount >= 4', 'MEDIUM', 'WARN', 5, 1, '短时间内频繁参与活动，疑似刷奖'),
(11, '重复领取优惠券', 'patternType == "COUPON_REPEAT"', 'HIGH', 'BLOCK', 8, 1, '重复领取优惠券，疑似羊毛党'),
(12, '异常渠道参与', 'channel == "bot" || source =~ "脚本"', 'HIGH', 'BLOCK', 10, 1, '通过机器人或脚本参与活动'),
(13, '超高参与次数', 'participationCount > 20', 'CRITICAL', 'BLOCK', 10, 1, '参与次数异常高，直接拦截');

-- 通用规则
INSERT INTO rule_config (id, name, expression, risk_level, action, weight, enabled, description) VALUES
(14, '未知模式类型', 'patternType == "UNKNOWN"', 'LOW', 'ALLOW', 1, 1, '未知模式类型，默认放行'),
(15, '测试规则（始终匹配）', 'true', 'LOW', 'ALLOW', 1, 0, '测试用规则，默认禁用');
