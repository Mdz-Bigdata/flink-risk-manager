package com.risk.rule;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * AviatorScript 规则引擎
 * 支持动态规则加载、编译缓存、规则执行
 */
public class AviatorRuleEngine {
    private static final Logger logger = LoggerFactory.getLogger(AviatorRuleEngine.class);

    private final Cache<String, Expression> expressionCache;
    private final Map<String, RuleConfig> ruleConfigs;

    public AviatorRuleEngine(int cacheSize, int cacheTTLSeconds) {
        this.expressionCache = CacheBuilder.newBuilder()
                .maximumSize(cacheSize)
                .expireAfterAccess(cacheTTLSeconds, TimeUnit.SECONDS)
                .build();
        this.ruleConfigs = new HashMap<>();
    }

    /**
     * 注册规则
     */
    public void registerRule(RuleConfig ruleConfig) {
        if (!ruleConfig.isEnabled()) {
            return;
        }
        ruleConfigs.put(ruleConfig.getId(), ruleConfig);
        logger.info("Rule registered: {} - {}", ruleConfig.getId(), ruleConfig.getName());
    }

    /**
     * 批量注册规则
     */
    public void registerRules(List<RuleConfig> rules) {
        for (RuleConfig rule : rules) {
            registerRule(rule);
        }
    }

    /**
     * 执行规则，返回匹配的规则
     */
    public List<RuleConfig> executeRules(Map<String, Object> context) {
        List<RuleConfig> matchedRules = new ArrayList<>();

        for (RuleConfig rule : ruleConfigs.values()) {
            if (!rule.isEnabled()) {
                continue;
            }

            try {
                boolean result = evaluateExpression(rule.getExpression(), context);
                if (result) {
                    matchedRules.add(rule);
                    logger.debug("Rule matched: {}", rule.getId());
                }
            } catch (Exception e) {
                logger.error("Error executing rule: {}", rule.getId(), e);
            }
        }

        return matchedRules;
    }

    /**
     * 评估单个表达式
     */
    public boolean evaluateExpression(String expression, Map<String, Object> context) {
        try {
            Expression compiledExpression = getOrCompileExpression(expression);
            Object result = compiledExpression.execute(context);
            return result instanceof Boolean ? (Boolean) result : false;
        } catch (Exception e) {
            logger.error("Error evaluating expression: {}", expression, e);
            return false;
        }
    }

    /**
     * 计算风险评分
     */
    public int calculateRiskScore(Map<String, Object> context) {
        List<RuleConfig> matchedRules = executeRules(context);
        int totalScore = 0;
        int totalWeight = 0;

        for (RuleConfig rule : matchedRules) {
            totalScore += rule.getRiskLevel().getScore() * rule.getWeight();
            totalWeight += rule.getWeight();
        }

        if (totalWeight == 0) {
            return 0;
        }

        return totalScore / totalWeight;
    }

    /**
     * 获取风险等级
     */
    public RiskLevel getRiskLevel(Map<String, Object> context) {
        int score = calculateRiskScore(context);
        return RiskLevel.fromScore(score);
    }

    /**
     * 获取或编译表达式
     */
    private Expression getOrCompileExpression(String expression) {
        try {
            return expressionCache.get(expression, () -> AviatorEvaluator.compile(expression));
        } catch (ExecutionException e) {
            logger.error("Failed to compile expression: {}", expression, e);
            throw new RuntimeException("Expression compilation failed", e);
        }
    }

    /**
     * 清空规则
     */
    public void clearRules() {
        ruleConfigs.clear();
        logger.info("All rules cleared");
    }

    /**
     * 移除规则
     */
    public void removeRule(String ruleId) {
        ruleConfigs.remove(ruleId);
        logger.info("Rule removed: {}", ruleId);
    }

    /**
     * 获取规则数量
     */
    public int getRuleCount() {
        return ruleConfigs.size();
    }

    /**
     * 获取所有规则
     */
    public Collection<RuleConfig> getAllRules() {
        return ruleConfigs.values();
    }
}
