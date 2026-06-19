package com.qinyadan.risk.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis 结果写入器（Flink 2.x 兼容版本）
 * 使用 foreach() 替代 addSink()
 */
public class RedisResultWriter {
    private static final Logger logger = LoggerFactory.getLogger(RedisResultWriter.class);

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

    public void open() {
        redisClient = new RedisClient(redisHost, redisPort, redisDatabase, timeout);
        logger.info("Redis writer opened");
    }

    public void write(RiskResult value) {
        try {
            if (redisClient == null) open();
            String key = value.getRedisKey();
            String jsonValue = value.toJsonString();
            redisClient.setWithTTL(key, jsonValue, value.getTtl());
            logger.debug("Risk result written to Redis: {} -> {}", key, jsonValue);
        } catch (Exception e) {
            logger.error("Error writing risk result to Redis", e);
        }
    }

    public void close() {
        if (redisClient != null) {
            redisClient.close();
        }
        logger.info("Redis writer closed");
    }
}
