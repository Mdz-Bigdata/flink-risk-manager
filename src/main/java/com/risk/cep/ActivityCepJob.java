package com.risk.cep;

import com.risk.event.ActivityEvent;
import com.risk.redis.RiskResult;
import com.risk.rule.AviatorRuleEngine;
import com.risk.rule.RiskLevel;
import com.risk.redis.RedisResultWriter;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * 活动风控 CEP 处理任务
 */
public class ActivityCepJob {
    private static final Logger logger = LoggerFactory.getLogger(ActivityCepJob.class);

    /**
     * 处理活动事件
     */
    public static DataStream<RiskResult> process(
            DataStream<ActivityEvent> input,
            AviatorRuleEngine ruleEngine,
            String redisHost,
            int redisPort,
            int redisDb) {

        // 模式1: 频繁参与活动（可能作弊）
        Pattern<ActivityEvent, ?> frequentPattern = PatternFactory.createActivityFrequentPattern();
        DataStream<RiskResult> frequentRisks = CEP.pattern(input, frequentPattern)
                .select((PatternSelectFunction<ActivityEvent, RiskResult>) pattern -> {
                    ActivityEvent firstEvent = pattern.get("first").get(0);
                    List<ActivityEvent> allEvents = pattern.get("fourth");
                    
                    RiskResult result = new RiskResult(
                            firstEvent.getUserId(),
                            UUID.randomUUID().toString(),
                            "ACTIVITY"
                    );
                    result.setRiskLevel(RiskLevel.HIGH);
                    result.setRiskScore(85);
                    result.setAction("BLOCK");
                    result.getDetails().put("participationCount", allEvents.size());
                    result.getDetails().put("reason", "Abnormal participation pattern detected");
                    
                    logger.info("Abnormal activity participation detected for user: {} with count: {}", 
                            firstEvent.getUserId(), allEvents.size());
                    return result;
                });

        // 模式2: 重复领取优惠券
        Pattern<ActivityEvent, ?> couponPattern = PatternFactory.createCouponRepeatPattern();
        DataStream<RiskResult> couponRisks = CEP.pattern(input, couponPattern)
                .select((PatternSelectFunction<ActivityEvent, RiskResult>) pattern -> {
                    ActivityEvent firstEvent = pattern.get("first").get(0);
                    
                    RiskResult result = new RiskResult(
                            firstEvent.getUserId(),
                            UUID.randomUUID().toString(),
                            "ACTIVITY"
                    );
                    result.setRiskLevel(RiskLevel.MEDIUM);
                    result.setRiskScore(70);
                    result.setAction("DISABLE");
                    result.getDetails().put("reason", "Repeated coupon claim detected");
                    
                    logger.info("Repeated coupon claim detected for user: {}", firstEvent.getUserId());
                    return result;
                });

        // 合并两个风险流
        DataStream<RiskResult> mergedRisks = frequentRisks.union(couponRisks);

        // 写入 Redis
        mergedRisks.addSink(new RedisResultWriter(redisHost, redisPort, redisDb, 2000));

        return mergedRisks;
    }
}