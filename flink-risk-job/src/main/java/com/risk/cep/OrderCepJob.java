package com.risk.cep;

import com.risk.event.OrderEvent;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

/**
 * 下单风控 CEP 处理任务（Flink 2.x 兼容）
 * 只负责 CEP 模式匹配，输出 PatternMatchResult 中间结果
 * 动态规则解析由下游 RuleEvaluationFunction（广播处理）完成
 */
public class OrderCepJob {
    private static final Logger logger = LoggerFactory.getLogger(OrderCepJob.class);

    public static DataStream<PatternMatchResult> process(DataStream<OrderEvent> input) {

        // 模式1: 频繁下单
        Pattern<OrderEvent, ?> frequentPattern = PatternFactory.createOrderFrequentPattern();
        DataStream<PatternMatchResult> frequentRisks = CEP.pattern(input, frequentPattern)
                .select((PatternSelectFunction<OrderEvent, PatternMatchResult>) pattern -> {
                    OrderEvent firstEvent = pattern.get("first").get(0);
                    List<OrderEvent> allEvents = pattern.get("third");

                    BigDecimal totalAmount = BigDecimal.ZERO;
                    for (OrderEvent event : allEvents) {
                        totalAmount = totalAmount.add(event.getAmount());
                    }

                    Map<String, Object> context = buildOrderContext(allEvents, "ORDER_FREQUENT");
                    context.put("totalAmount", totalAmount.doubleValue());

                    Map<String, Object> details = new HashMap<>();
                    details.put("orderCount", allEvents.size());
                    details.put("totalAmount", totalAmount);

                    PatternMatchResult result = new PatternMatchResult(
                            firstEvent.getUserId(), "ORDER", "ORDER_FREQUENT");
                    result.setEventId(UUID.randomUUID().toString());
                    result.setContext(context);
                    result.setDetails(details);

                    logger.info("Frequent order detected: user={}, count={}, amount={}",
                            firstEvent.getUserId(), allEvents.size(), totalAmount);
                    return result;
                });

        // 模式2: 快速连续下单
        Pattern<OrderEvent, ?> rapidPattern = PatternFactory.createOrderRapidPattern();
        DataStream<PatternMatchResult> rapidRisks = CEP.pattern(input, rapidPattern)
                .select((PatternSelectFunction<OrderEvent, PatternMatchResult>) pattern -> {
                    List<OrderEvent> firstList = pattern.get("first");
                    List<OrderEvent> secondList = pattern.get("second");
                    OrderEvent firstEvent = firstList.get(0);
                    OrderEvent secondEvent = secondList.get(0);

                    long interval = secondEvent.getTimestamp() - firstEvent.getTimestamp();

                    Map<String, Object> context = buildOrderContext(secondList, "ORDER_RAPID");
                    context.put("intervalMs", (double) interval);

                    Map<String, Object> details = new HashMap<>();
                    details.put("reason", "Rapid order submission detected");
                    details.put("intervalMs", interval);
                    details.put("firstOrderId", firstEvent.getOrderId());
                    details.put("secondOrderId", secondEvent.getOrderId());

                    PatternMatchResult result = new PatternMatchResult(
                            firstEvent.getUserId(), "ORDER", "ORDER_RAPID");
                    result.setEventId(UUID.randomUUID().toString());
                    result.setContext(context);
                    result.setDetails(details);

                    logger.info("Rapid order detected: user={}, interval={}ms",
                            firstEvent.getUserId(), interval);
                    return result;
                });

        return frequentRisks.union(rapidRisks);
    }

    private static Map<String, Object> buildOrderContext(List<OrderEvent> events, String patternType) {
        Map<String, Object> context = new HashMap<>();
        if (events != null && !events.isEmpty()) {
            OrderEvent latest = events.get(events.size() - 1);
            context.put("eventType", "ORDER");
            context.put("patternType", patternType);
            context.put("userId", latest.getUserId());
            context.put("orderCount", events.size());
            context.put("amount", latest.getAmount().doubleValue());
            context.put("productId", latest.getProductId());
            context.put("paymentMethod", latest.getPaymentMethod());
        }
        return context;
    }
}
