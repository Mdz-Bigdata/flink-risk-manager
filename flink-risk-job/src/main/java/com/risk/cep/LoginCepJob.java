package com.risk.cep;

import com.risk.event.LoginEvent;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 登录风控 CEP 处理任务（Flink 2.x 兼容）
 * 只负责 CEP 模式匹配，输出 PatternMatchResult 中间结果
 * 动态规则解析由下游 RuleEvaluationFunction（广播处理）完成
 */
public class LoginCepJob {
    private static final Logger logger = LoggerFactory.getLogger(LoginCepJob.class);

    public static DataStream<PatternMatchResult> process(DataStream<LoginEvent> input) {

        // 模式1: 登录失败多次
        Pattern<LoginEvent, ?> failurePattern = PatternFactory.createLoginFailurePattern();
        DataStream<PatternMatchResult> failureRisks = CEP.pattern(input, failurePattern)
                .select((PatternSelectFunction<LoginEvent, PatternMatchResult>) pattern -> {
                    LoginEvent firstEvent = pattern.get("first").get(0);
                    List<LoginEvent> allEvents = pattern.get("third");

                    PatternMatchResult result = new PatternMatchResult(
                            firstEvent.getUserId(), "LOGIN", "LOGIN_FAILURE");
                    result.setEventId(UUID.randomUUID().toString());
                    result.setContext(buildLoginContext(allEvents, "LOGIN_FAILURE"));
                    result.setDetails(buildFailureDetails(allEvents));

                    logger.info("Login failure pattern detected: user={}, failCount={}",
                            firstEvent.getUserId(), allEvents.size());
                    return result;
                });

        // 模式2: 异地登录
        Pattern<LoginEvent, ?> locationPattern = PatternFactory.createLoginLocationChangePattern();
        DataStream<PatternMatchResult> locationRisks = CEP.pattern(input, locationPattern)
                .select((PatternSelectFunction<LoginEvent, PatternMatchResult>) pattern -> {
                    List<LoginEvent> firstList = pattern.get("first");
                    List<LoginEvent> secondList = pattern.get("second");
                    LoginEvent prevEvent = firstList.get(firstList.size() - 1);
                    LoginEvent currEvent = secondList.get(0);

                    PatternMatchResult result = new PatternMatchResult(
                            currEvent.getUserId(), "LOGIN", "LOCATION_CHANGE");
                    result.setEventId(UUID.randomUUID().toString());

                    Map<String, Object> context = buildLoginContext(secondList, "LOCATION_CHANGE");
                    context.put("prevIp", prevEvent.getIp());
                    context.put("currIp", currEvent.getIp());
                    result.setContext(context);

                    Map<String, Object> details = new HashMap<>();
                    details.put("prevIp", prevEvent.getIp());
                    details.put("currIp", currEvent.getIp());
                    details.put("prevLocation", prevEvent.getLocation());
                    details.put("currLocation", currEvent.getLocation());
                    result.setDetails(details);

                    logger.info("Location change detected: user={}, prev={}, curr={}",
                            currEvent.getUserId(), prevEvent.getIp(), currEvent.getIp());
                    return result;
                });

        return failureRisks.union(locationRisks);
    }

    private static Map<String, Object> buildLoginContext(List<LoginEvent> events, String patternType) {
        Map<String, Object> context = new HashMap<>();
        if (events != null && !events.isEmpty()) {
            LoginEvent latest = events.get(events.size() - 1);
            context.put("eventType", "LOGIN");
            context.put("patternType", patternType);
            context.put("userId", latest.getUserId());
            context.put("failCount", events.size());
            context.put("ip", latest.getIp());
            context.put("deviceId", latest.getDeviceId());
            context.put("success", latest.isSuccess());
            context.put("location", latest.getLocation());
            context.put("browser", latest.getBrowser());
        }
        return context;
    }

    private static Map<String, Object> buildFailureDetails(List<LoginEvent> events) {
        Map<String, Object> details = new HashMap<>();
        details.put("failCount", events.size());
        if (!events.isEmpty()) {
            details.put("lastIp", events.get(events.size() - 1).getIp());
            details.put("lastDeviceId", events.get(events.size() - 1).getDeviceId());
        }
        return details;
    }
}
