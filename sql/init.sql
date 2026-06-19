-- 规则配置表
CREATE TABLE IF NOT EXISTS rule_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    expression TEXT NOT NULL COMMENT 'Aviator 表达式',
    risk_level VARCHAR(20) NOT NULL COMMENT '风险等级: LOW/MEDIUM/HIGH/CRITICAL',
    action VARCHAR(20) NOT NULL COMMENT '动作: ALLOW/VERIFY/BLOCK',
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
