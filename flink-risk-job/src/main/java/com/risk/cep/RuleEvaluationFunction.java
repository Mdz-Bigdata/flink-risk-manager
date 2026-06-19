package com.risk.cep;

import com.risk.redis.RiskResult;
import com.risk.rule.AviatorRuleEngine;
import com.risk.rule.RiskActionResolver;
import com.risk.rule.RuleConfig;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 规则评估广播处理函数
 *
 * 双流连接：
 *   - 主输入流：PatternMatchResult（CEP 模式匹配命中后的中间结果）
 *   - 广播流：List<RuleConfig>（从 MySQL 定时拉取的规则快照）
 *
 * 工作方式：
 *   1. processElement：收到模式匹配结果时，从 BroadcastState 读取最新规则，
 *      通过 AviatorRuleEngine 动态评估，输出 RiskResult
 *   2. processBroadcastElement：收到新的规则快照时，全量替换 BroadcastState，
 *      重建 AviatorRuleEngine 实例
 */
public class RuleEvaluationFunction
        extends BroadcastProcessFunction<PatternMatchResult, List<RuleConfig>, RiskResult> {

    private static final Logger logger = LoggerFactory.getLogger(RuleEvaluationFunction.class);

    /** 广播状态 Key */
    public static final String RULE_STATE_NAME = "rule-config-state";
    public static final MapStateDescriptor<Void, List<RuleConfig>> RULE_STATE_DESCRIPTOR =
            new MapStateDescriptor<>(
                    RULE_STATE_NAME,
                    BasicTypeInfo.VOID_TYPE_INFO,
                    TypeInformation.of(new TypeHint<List<RuleConfig>>() {})
            );

    /** 每个并行实例持有的规则引擎 */
    private transient volatile AviatorRuleEngine ruleEngine;
    private transient volatile RiskActionResolver resolver;

    @Override
    public void open(OpenContext openContext) throws Exception {
        super.open(openContext);
        // 初始化空引擎，等待第一次广播规则到达
        this.ruleEngine = new AviatorRuleEngine(1000, 3600);
        this.resolver = new RiskActionResolver(ruleEngine);
        logger.info("RuleEvaluationFunction opened, waiting for rule broadcast...");
    }

    /**
     * 处理主输入流：CEP 模式匹配结果
     * 从广播状态获取最新规则，通过引擎动态评估 action
     */
    @Override
    public void processElement(
            PatternMatchResult matchResult,
            ReadOnlyContext ctx,
            Collector<RiskResult> out) throws Exception {

        logger.info("[EVAL] Processing match: user={}, eventType={}, pattern={}, context={}",
                matchResult.getUserId(), matchResult.getEventType(),
                matchResult.getPatternType(), matchResult.getContext());

        // ReadOnlyContext.getBroadcastState 返回 ReadOnlyBroadcastState
        ReadOnlyBroadcastState<Void, List<RuleConfig>> broadcastState =
                ctx.getBroadcastState(RULE_STATE_DESCRIPTOR);

        // Void 无法实例化，用 null 作为广播状态的唯一 Key
        List<RuleConfig> rules = broadcastState.get(null);

        if (rules == null || rules.isEmpty()) {
            // 广播规则尚未到达，输出默认低风险结果
            RiskResult result = new RiskResult(
                    matchResult.getUserId(),
                    matchResult.getEventId(),
                    matchResult.getEventType()
            );
            result.setRiskLevel(com.risk.rule.RiskLevel.LOW);
            result.setRiskScore(0);
            result.setAction("ALLOW");
            result.getDetails().put("reason", "No rules loaded yet");
            result.getDetails().putAll(matchResult.getDetails());
            out.collect(result);
            logger.warn("[EVAL] No rules in broadcast state, output ALLOW for user: {}", matchResult.getUserId());
            return;
        }

        logger.info("[EVAL] {} rules available, evaluating context against rules...", rules.size());

        // 通过规则引擎动态解析
        RiskResult result = resolver.resolve(
                matchResult.getContext(),
                matchResult.getUserId(),
                matchResult.getEventId(),
                matchResult.getEventType(),
                matchResult.getDetails()
        );

        logger.info("[EVAL] Result: user={}, action={}, level={}, score={}, details={}",
                result.getUserId(), result.getAction(),
                result.getRiskLevel(), result.getRiskScore(), result.getDetails());

        out.collect(result);
    }

    /**
     * 处理广播流：规则快照更新
     * 全量替换广播状态，并重建本地规则引擎
     */
    @Override
    public void processBroadcastElement(
            List<RuleConfig> rules,
            Context ctx,
            Collector<RiskResult> out) throws Exception {

        // 更新广播状态
        BroadcastState<Void, List<RuleConfig>> broadcastState =
                ctx.getBroadcastState(RULE_STATE_DESCRIPTOR);
        broadcastState.put(null, rules);

        // 重建本地规则引擎
        AviatorRuleEngine newEngine = new AviatorRuleEngine(1000, 3600);
        newEngine.registerRules(rules);
        this.ruleEngine = newEngine;
        this.resolver = new RiskActionResolver(newEngine);

        logger.info("Rule engine refreshed in this task: {} rules loaded", newEngine.getRuleCount());
    }
}
