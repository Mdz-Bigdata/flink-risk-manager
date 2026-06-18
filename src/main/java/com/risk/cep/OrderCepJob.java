package com.risk.cep;

import com.risk.event.OrderEvent;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 下单风控 CEP 处理任务
 */
public class OrderCepJob {
    private static final Logger logger = LoggerFactory.getLogger(OrderCepJob.class);

    /**
     * 处理订单事件
     */
    public static DataStream<RiskResult> process(
            DataStream<OrderEvent> input,
            AviatorRuleEngine ruleEngine,
            String redisHost,
            int redisPort,
            int redisDb) {

        // 模式1: 频繁下单
        Pattern<OrderEvent, ?> frequentPattern = PatternFactory.createOrderFrequentPattern();
        DataStream<RiskResult> frequentRisks = CEP.pattern(input, frequentPattern)
                .select((PatternSelectFunction<OrderEvent, RiskResult>) pattern -> {
                    OrderEvent firstEvent = pattern.get("first").get(0);
                    List<OrderEvent> allEvents = pattern.get("third");
                    
                    BigDecimal totalAmount = BigDecimal.ZERO;
                    for (OrderEvent event : allEvents) {
                        totalAmount = totalAmount.add(event.getAmount());
                    }
                    
                    RiskResult result = new RiskResult(
                            firstEvent.getUserId(),
                            UUID.randomUUID().toString(),
                            "ORDER"
                    );
                    result.setRiskLevel(RiskLevel.MEDIUM);
                    result.setRiskScore(65);
                    result.setAction("LIMIT");
                    result.getDetails().put("count", allEvents.size());
                    result.getDetails().put("totalAmount", totalAmount);
                    
                    logger.info("Frequent order pattern detected for user: {} with amount: {}", 
                            firstEvent.getUserId(), totalAmount);
                    return result;
                });

        // 模式2: 快速连续下单
        Pattern<OrderEvent, ?> rapidPattern = PatternFactory.createOrderRapidPattern();
        DataStream<RiskResult> rapidRisks = CEP.pattern(input, rapidPattern)
                .select((PatternSelectFunction<OrderEvent, RiskResult>) pattern -> {
                    OrderEvent firstEvent = pattern.get("first").get(0);
                    RiskResult result = new RiskResult(
                            firstEvent.getUserId(),
                            UUID.randomUUID().toString(),
                            "ORDER"
                    );
                    result.setRiskLevel(RiskLevel.HIGH);
                    result.setRiskScore(80);
                    result.setAction("REVIEW");
                    result.getDetails().put("reason", "Rapid order submission detected");
                    
                    logger.info("Rapid order pattern detected for user: {}", firstEvent.getUserId());
                    return result;
                });

        // 合并两个风险流
        DataStream<RiskResult> mergedRisks = frequentRisks.union(rapidRisks);

        // 写入 Redis
        mergedRisks.addSink(new RedisResultWriter(redisHost, redisPort, redisDb, 2000));

        return mergedRisks;
    }
}