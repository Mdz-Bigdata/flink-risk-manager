package com.qinyadan.risk.cep;

import com.qinyadan.risk.rule.RuleConfig;
import com.qinyadan.risk.rule.RiskLevel;
import org.apache.flink.streaming.api.functions.source.legacy.SourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 规则数据源
 * 从 MySQL rule_config 表定时轮询拉取规则，作为广播流的数据源
 *
 * 工作方式：
 *   - 只有一个并行度（parallelism=1），避免重复查询数据库
 *   - 启动时立即拉取一次
 *   - 之后按固定间隔轮询
 *   - 通过 AtomicReference 持有最新规则快照
 */
public class RuleSourceFunction implements SourceFunction<List<RuleConfig>> {

    private static final Logger logger = LoggerFactory.getLogger(RuleSourceFunction.class);

    /** 规则快照，供外部读取 */
    private final AtomicReference<List<RuleConfig>> ruleSnapshot = new AtomicReference<>(new ArrayList<>());

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final long intervalMs;

    private volatile boolean running = true;

    public RuleSourceFunction(String jdbcUrl, String username, String password, long intervalMs) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.intervalMs = intervalMs;
    }

    @Override
    public void run(SourceContext<List<RuleConfig>> ctx) throws Exception {
        while (running) {
            try {
                List<RuleConfig> rules = fetchRulesFromDB();
                ruleSnapshot.set(rules);
                ctx.collect(rules);
                logger.info("Broadcast rules snapshot: {} rules", rules.size());
            } catch (Exception e) {
                logger.error("Failed to fetch rules from MySQL, will retry in {}ms", intervalMs, e);
            }

            // 等待下一次轮询
            Thread.sleep(intervalMs);
        }
    }

    @Override
    public void cancel() {
        running = false;
    }

    /**
     * 获取当前规则快照（供非广播场景使用）
     */
    public List<RuleConfig> getRuleSnapshot() {
        return ruleSnapshot.get();
    }

    /**
     * 从 MySQL 查询所有启用的规则
     */
    private List<RuleConfig> fetchRulesFromDB() throws SQLException {
        List<RuleConfig> rules = new ArrayList<>();

        String sql = "SELECT id, name, expression, risk_level, action, weight, enabled, description " +
                     "FROM rule_config WHERE enabled = 1";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                RuleConfig config = new RuleConfig();
                config.setId(String.valueOf(rs.getLong("id")));
                config.setName(rs.getString("name"));
                config.setExpression(rs.getString("expression"));
                config.setRiskLevel(RiskLevel.valueOf(rs.getString("risk_level")));
                config.setAction(rs.getString("action"));
                config.setWeight(rs.getInt("weight"));
                config.setEnabled(rs.getBoolean("enabled"));
                config.setDescription(rs.getString("description"));
                rules.add(config);
            }
        }

        return rules;
    }
}
