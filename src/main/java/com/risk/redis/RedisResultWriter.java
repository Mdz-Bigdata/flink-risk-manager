package com.risk.redis;

import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis 结果写入器
 * 将风控结果写入 Redis
 */
public class RedisResultWriter extends RichSinkFunction<RiskResult> {
    private static final Logger logger = LoggerFactory.getLogger(RedisResultWriter.java);

    private final String redisHost;
    private final int redisPort;
    private final int redisDatabase;
    private final int timeout;

    private transient RedisClient redisClient;

    public RedisResultWriter(String redisHost, int redisPort, int redisDatabase, int timeout) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisDatabase = redisDatabase;
        this.timeout = timeout;
    }

    @Override
    public void open(org.apache.flink.configuration.Configuration parameters) throws Exception {
        super.open(parameters);
        redisClient = new RedisClient(redisHost, redisPort, redisDatabase, timeout);
        logger.info("Redis writer opened");
    }

    @Override
    public void invoke(RiskResult value, Context context) throws Exception {
        try {
            String key = value.getRedisKey();
            String jsonValue = value.toJsonString();
            
            // 写入 Redis，设置 TTL
            redisClient.setWithTTL(key, jsonValue, value.getTtl());
            
            logger.debug("Risk result written to Redis: {} -> {}", key, jsonValue);
        } catch (Exception e) {
            logger.error("Error writing risk result to Redis", e);
        }
    }

    @Override
    public void close() throws Exception {
        if (redisClient != null) {
            redisClient.close();
        }
        super.close();
        logger.info("Redis writer closed");
    }
}