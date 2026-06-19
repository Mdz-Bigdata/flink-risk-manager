package com.risk.rule;

import com.risk.redis.RiskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 风险动作解析器
 * CEP 模式匹配后，将事件上下文交给规则引擎执行，
 * 根据匹配到的规则动态决定 action、riskLevel、riskScore
 */
public class RiskActionResolver {
    private static final Logger logger = LoggerFactory.getLogger(RiskActionResolver.class);

    private final AviatorRuleEngine ruleEngine;

    public RiskActionResolver(AviatorRuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    /**
     * 根据事件上下文，通过规则引擎动态解析风险结果
     *
     * @param context   事件上下文变量（供 Aviator 表达式引用）
     * @param userId    用户ID
     * @param eventId   事件ID
     * @param eventType 事件类型
     * @param details   额外详情
     * @return 填充了动态 action/riskLevel/riskScore 的 RiskResult
     */
    public RiskResult resolve(Map<String, Object> context,
                              String userId,
                              String eventId,
                              String eventType,
                              Map<String, Object> details) {
        RiskResult result = new RiskResult(userId, eventId, eventType);

        // 通过规则引擎执行所有匹配规则
        List<RuleConfig> matchedRules = ruleEngine.executeRules(context);

        if (matchedRules.isEmpty()) {
            // 无规则匹配时，使用默认低风险
            result.setRiskLevel(RiskLevel.LOW);
            result.setRiskScore(0);
            result.setAction("ALLOW");
            result.getDetails().put("reason", "No rule matched");
            logger.debug("No rule matched for user: {} event: {}", userId, eventType);
        } else {
            // 取优先级最高的规则（风险评分最高的）
            RuleConfig topRule = selectTopPriorityRule(matchedRules);

            result.setRiskLevel(topRule.getRiskLevel());
            result.setRiskScore(topRule.getRiskLevel().getScore());
            result.setAction(topRule.getAction());
            result.getDetails().put("matchedRuleId", topRule.getId());
            result.getDetails().put("matchedRuleName", topRule.getName());
            result.getDetails().put("matchedRuleCount", matchedRules.size());

            if (details != null) {
                result.getDetails().putAll(details);
            }

            logger.info("Rule engine resolved action for user: {} event: {} -> rule: {} action: {} level: {}",
                    userId, eventType, topRule.getName(), topRule.getAction(), topRule.getRiskLevel());
        }

        return result;
    }

    /**
     * 从匹配的规则中选择优先级最高的规则
     * 优先级：按 riskLevel 的 score 降序，score 相同按 weight 降序
     */
    private RuleConfig selectTopPriorityRule(List<RuleConfig> matchedRules) {
        return matchedRules.stream()
                .max((a, b) -> {
                    int scoreCompare = Integer.compare(
                            a.getRiskLevel().getScore(), b.getRiskLevel().getScore());
                    if (scoreCompare != 0) return scoreCompare;
                    return Integer.compare(a.getWeight(), b.getWeight());
                })
                .orElse(matchedRules.get(0));
    }

    /**
     * 获取规则引擎（供外部查询规则数量等）
     */
    public AviatorRuleEngine getRuleEngine() {
        return ruleEngine;
    }
}
