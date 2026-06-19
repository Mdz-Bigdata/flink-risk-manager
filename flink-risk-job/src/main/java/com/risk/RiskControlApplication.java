package com.risk;

import com.risk.cep.*;
import com.risk.event.ActivityEvent;
import com.risk.event.LoginEvent;
import com.risk.event.OrderEvent;
import com.risk.kafka.KafkaSourceFactory;
import com.risk.redis.RiskResult;
import com.risk.rule.RuleConfig;
import com.risk.sink.KafkaSinkFactory;
import com.risk.sink.MysqlSinkFunction;
import com.risk.sink.RedisSinkFunction;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.List;

/**
 * 风控主应用 - Flink 2.2.1
 *
 * 架构：Kafka 事件源 -> CEP 模式匹配 -> 广播规则评估 -> 三路输出（Redis + MySQL + Kafka）
 *
 * 启动命令:
 *   flink run -c com.risk.RiskControlApplication flink-risk-job-2.0.1-SNAPSHOT.jar \
 *     --kafka.bootstrap.servers kafka:9092 \
 *     --mysql.url jdbc:mysql://mysql:3306/risk_control?useSSL=false&serverTimezone=UTC \
 *     --mysql.username root --mysql.password root123 \
 *     --redis.host redis --redis.port 6379 --redis.db 0 \
 *     --rule.poll.interval 30000
 *
 * 流程:
 *   1. KafkaSource 消费 login-events / order-events / activity-events
 *   2. 规则广播流从 MySQL 定时拉取（parallelism=1）
 *   3. CEP 模式匹配产生 PatternMatchResult 中间结果
 *   4. RuleEvaluationFunction 连接广播规则，通过 AviatorRuleEngine 动态评估
 *   5. RiskResult 三路输出：
 *      - Redis：实时缓存（供实时查询）
 *      - MySQL：异步持久化（供历史分析、Web 后台查询）
 *      - Kafka：异步发送到 risk-results topic（供下游系统消费）
 */
public class RiskControlApplication {
    public static void main(String[] args) throws Exception {
        // ========== 1. 读取配置 ==========
        String kafkaBootstrapServers = getArg(args, "--kafka.bootstrap.servers", "localhost:9092");
        String mysqlUrl = getArg(args, "--mysql.url", "jdbc:mysql://localhost:3306/risk_control?useSSL=false&serverTimezone=UTC");
        String mysqlUsername = getArg(args, "--mysql.username", "root");
        String mysqlPassword = getArg(args, "--mysql.password", "root");
        String redisHost = getArg(args, "--redis.host", "localhost");
        int redisPort = Integer.parseInt(getArg(args, "--redis.port", "6379"));
        int redisDb = Integer.parseInt(getArg(args, "--redis.db", "0"));
        long rulePollInterval = Long.parseLong(getArg(args, "--rule.poll.interval", "30000"));

        // ========== 2. 创建 Flink 执行环境 ==========
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(4);
        env.enableCheckpointing(60000);

        System.out.println("Flink Risk Control Job Started. Flink version: 2.2.1");
        System.out.println("Kafka: " + kafkaBootstrapServers);
        System.out.println("MySQL: " + mysqlUrl);
        System.out.println("Redis: " + redisHost + ":" + redisPort + "/" + redisDb);
        System.out.println("Rule poll interval: " + rulePollInterval + "ms");

        // ========== 3. 规则广播流（parallelism=1，避免重复查库） ==========
        DataStreamSource<List<RuleConfig>> ruleStream = env
                .addSource(new RuleSourceFunction(mysqlUrl, mysqlUsername, mysqlPassword, rulePollInterval))
                .setParallelism(1)
                .name("mysql-rule-source");

        BroadcastStream<List<RuleConfig>> ruleBroadcastStream = ruleStream
                .broadcast(RuleEvaluationFunction.RULE_STATE_DESCRIPTOR);

        // ========== 4. Kafka 事件源（新 Source API） ==========
        DataStream<LoginEvent> loginStream = env
                .fromSource(
                        KafkaSourceFactory.createLoginSource(kafkaBootstrapServers),
                        WatermarkStrategy.noWatermarks(),
                        "kafka-login-source")
                .name("login-events")
                .setParallelism(2);

        DataStream<OrderEvent> orderStream = env
                .fromSource(
                        KafkaSourceFactory.createOrderSource(kafkaBootstrapServers),
                        WatermarkStrategy.noWatermarks(),
                        "kafka-order-source")
                .name("order-events")
                .setParallelism(2);

        DataStream<ActivityEvent> activityStream = env
                .fromSource(
                        KafkaSourceFactory.createActivitySource(kafkaBootstrapServers),
                        WatermarkStrategy.noWatermarks(),
                        "kafka-activity-source")
                .name("activity-events")
                .setParallelism(2);

        // ========== 5. CEP 模式匹配 ==========
        DataStream<PatternMatchResult> loginMatches = LoginCepJob.process(loginStream);
        DataStream<PatternMatchResult> orderMatches = OrderCepJob.process(orderStream);
        DataStream<PatternMatchResult> activityMatches = ActivityCepJob.process(activityStream);

        DataStream<PatternMatchResult> allMatches = loginMatches.union(orderMatches, activityMatches);

        // ========== 6. 连接广播流，动态规则评估 ==========
        DataStream<RiskResult> riskResults = allMatches
                .connect(ruleBroadcastStream)
                .process(new RuleEvaluationFunction())
                .name("rule-evaluation");

        // ========== 7. 三路输出 ==========

        // 7a. Redis Sink（实时缓存，供实时查询）
        riskResults
                .sinkTo(new RedisSinkFunction(redisHost, redisPort, redisDb))
                .name("redis-sink");

        // 7b. MySQL Sink（异步持久化，供历史分析和 Web 后台查询）
        riskResults
                .process(new MysqlSinkFunction(mysqlUrl, mysqlUsername, mysqlPassword))
                .name("mysql-async-sink");

        // 7c. Kafka Sink（异步发送到 risk-results topic，供下游系统消费）
        riskResults
                .sinkTo(KafkaSinkFactory.createRiskResultSink(kafkaBootstrapServers))
                .name("kafka-risk-result-sink");

        env.execute("flink-risk-control-job");
    }

    private static String getArg(String[] args, String key, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(key)) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }
}
