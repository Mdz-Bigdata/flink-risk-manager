package com.risk.cep;

import com.risk.event.LoginEvent;
import com.risk.redis.RiskResult;
import com.risk.rule.AviatorRuleEngine;
import com.risk.rule.RiskLevel;
import com.risk.redis.RedisResultWriter;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 登录风控 CEP 处理任务
 */
public class LoginCepJob {
    private static final Logger logger = LoggerFactory.getLogger(LoginCepJob.class);

    /**
     * 处理登录事件
     */
    public static DataStream<RiskResult> process(
            DataStream<LoginEvent> input,
            AviatorRuleEngine ruleEngine,
            String redisHost,
            int redisPort,
            int redisDb) {

        // 模式1: 登录失败多次
        Pattern<LoginEvent, ?> failurePattern = PatternFactory.createLoginFailurePattern();
        DataStream<RiskResult> failureRisks = CEP.pattern(input, failurePattern)
                .select((PatternSelectFunction<LoginEvent, RiskResult>) pattern -> {
                    LoginEvent firstEvent = pattern.get("first").get(0);
                    RiskResult result = new RiskResult(
                            firstEvent.getUserId(),
                            UUID.randomUUID().toString(),
                            "LOGIN"
                    );
                    result.setRiskLevel(RiskLevel.HIGH);
                    result.setRiskScore(85);
                    result.setAction("BLOCK");
                    logger.info("Login failure pattern detected for user: {}", firstEvent.getUserId());
                    return result;
                });

        // 模式2: 异地登录
        Pattern<LoginEvent, ?> locationPattern = PatternFactory.createLoginLocationChangePattern();
        DataStream<RiskResult> locationRisks = CEP.pattern(input, locationPattern)
                .select((PatternSelectFunction<LoginEvent, RiskResult>) pattern -> {
                    List<LoginEvent> events = pattern.get("second");
                    LoginEvent firstEvent = events.get(0);
                    RiskResult result = new RiskResult(
                            firstEvent.getUserId(),
                            UUID.randomUUID().toString(),
                            "LOGIN"
                    );
                    result.setRiskLevel(RiskLevel.MEDIUM);
                    result.setRiskScore(60);
                    result.setAction("VERIFY");
                    logger.info("Location change detected for user: {}", firstEvent.getUserId());
                    return result;
                });

        // 合并两个风险流
        DataStream<RiskResult> mergedRisks = failureRisks.union(locationRisks);

        // 写入 Redis
        mergedRisks.addSink(new RedisResultWriter(redisHost, redisPort, redisDb, 2000));

        return mergedRisks;
    }
}