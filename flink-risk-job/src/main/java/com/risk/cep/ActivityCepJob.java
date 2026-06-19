package com.risk.cep;

import com.risk.event.ActivityEvent;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 活动风控 CEP 处理任务（Flink 2.x 兼容）
 * 只负责 CEP 模式匹配，输出 PatternMatchResult 中间结果
 * 动态规则解析由下游 RuleEvaluationFunction（广播处理）完成
 */
public class ActivityCepJob {
    private static final Logger logger = LoggerFactory.getLogger(ActivityCepJob.class);

    public static DataStream<PatternMatchResult> process(DataStream<ActivityEvent> input) {

        // 模式1: 频繁参与活动
        Pattern<ActivityEvent, ?> frequentPattern = PatternFactory.createActivityFrequentPattern();
        DataStream<PatternMatchResult> frequentRisks = CEP.pattern(input, frequentPattern)
                .select((PatternSelectFunction<ActivityEvent, PatternMatchResult>) pattern -> {
                    ActivityEvent firstEvent = pattern.get("first").get(0);
                    List<ActivityEvent> allEvents = pattern.get("fourth");

                    Map<String, Object> context = buildActivityContext(allEvents, "ACTIVITY_FREQUENT");

                    Map<String, Object> details = new HashMap<>();
                    details.put("participationCount", allEvents.size());
                    details.put("reason", "Abnormal participation pattern detected");

                    PatternMatchResult result = new PatternMatchResult(
                            firstEvent.getUserId(), "ACTIVITY", "ACTIVITY_FREQUENT");
                    result.setEventId(UUID.randomUUID().toString());
                    result.setContext(context);
                    result.setDetails(details);

                    logger.info("Abnormal activity detected: user={}, count={}",
                            firstEvent.getUserId(), allEvents.size());
                    return result;
                });

        // 模式2: 重复领取优惠券
        Pattern<ActivityEvent, ?> couponPattern = PatternFactory.createCouponRepeatPattern();
        DataStream<PatternMatchResult> couponRisks = CEP.pattern(input, couponPattern)
                .select((PatternSelectFunction<ActivityEvent, PatternMatchResult>) pattern -> {
                    ActivityEvent firstEvent = pattern.get("first").get(0);
                    List<ActivityEvent> allEvents = pattern.get("third");

                    Map<String, Object> context = buildActivityContext(allEvents, "COUPON_REPEAT");

                    Map<String, Object> details = new HashMap<>();
                    details.put("reason", "Repeated coupon claim detected");
                    details.put("couponClaimCount", allEvents.size());
                    if (!allEvents.isEmpty()) {
                        details.put("couponCode", allEvents.get(0).getCouponCode());
                    }

                    PatternMatchResult result = new PatternMatchResult(
                            firstEvent.getUserId(), "ACTIVITY", "COUPON_REPEAT");
                    result.setEventId(UUID.randomUUID().toString());
                    result.setContext(context);
                    result.setDetails(details);

                    logger.info("Repeated coupon claim detected: user={}",
                            firstEvent.getUserId());
                    return result;
                });

        return frequentRisks.union(couponRisks);
    }

    private static Map<String, Object> buildActivityContext(List<ActivityEvent> events, String patternType) {
        Map<String, Object> context = new HashMap<>();
        if (events != null && !events.isEmpty()) {
            ActivityEvent latest = events.get(events.size() - 1);
            context.put("eventType", "ACTIVITY");
            context.put("patternType", patternType);
            context.put("userId", latest.getUserId());
            context.put("participationCount", events.size());
            context.put("activityId", latest.getActivityId());
            context.put("activityName", latest.getActivityName());
            context.put("actionType", latest.getActionType());
            context.put("channel", latest.getChannel());
            context.put("source", latest.getSource());
        }
        return context;
    }
}
