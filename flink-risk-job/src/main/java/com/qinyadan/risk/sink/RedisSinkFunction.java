package com.qinyadan.risk.sink;

import com.qinyadan.risk.redis.RiskResult;
import org.apache.flink.api.connector.sink2.Sink;
import org.apache.flink.api.connector.sink2.SinkWriter;
import org.apache.flink.api.connector.sink2.WriterInitContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Redis Sink（Flink 2.x 新 Sink API）
 * 将 RiskResult 写入 Redis，key 格式：risk:{eventType}:{userId}:{eventId}
 *
 * 使用方式：
 *   riskResults.sinkTo(new RedisSinkFunction(host, port, db));
 */
public class RedisSinkFunction implements Sink<RiskResult> {

    private final String redisHost;
    private final int redisPort;
    private final int redisDatabase;
    private final int timeout;

    public RedisSinkFunction(String redisHost, int redisPort, int redisDatabase) {
        this(redisHost, redisPort, redisDatabase, 2000);
    }

    public RedisSinkFunction(String redisHost, int redisPort, int redisDatabase, int timeout) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisDatabase = redisDatabase;
        this.timeout = timeout;
    }

    @Override
    public SinkWriter<RiskResult> createWriter(WriterInitContext context) throws IOException {
        return new RedisSinkWriter(redisHost, redisPort, redisDatabase, timeout);
    }

    // ==================== SinkWriter 实现 ====================

    static class RedisSinkWriter implements SinkWriter<RiskResult> {
        private static final Logger logger = LoggerFactory.getLogger(RedisSinkWriter.class);

        private final com.qinyadan.risk.redis.RedisClient redisClient;

        RedisSinkWriter(String redisHost, int redisPort, int redisDatabase, int timeout) {
            this.redisClient = new com.qinyadan.risk.redis.RedisClient(redisHost, redisPort, redisDatabase, timeout);
            logger.info("RedisSinkWriter opened: {}:{}", redisHost, redisPort);
        }

        @Override
        public void write(RiskResult value, Context context) throws IOException {
            try {
                String key = value.getRedisKey();
                String jsonValue = value.toJsonString();
                redisClient.setWithTTL(key, jsonValue, value.getTtl());
                if (logger.isDebugEnabled()) {
                    logger.debug("Risk result written to Redis: {}", key);
                }
            } catch (Exception e) {
                logger.error("Error writing risk result to Redis: user={}, event={}",
                        value.getUserId(), value.getEventType(), e);
            }
        }

        @Override
        public void flush(boolean endOfInput) throws IOException {
            // Redis 是逐条写入的，flush 无需额外操作
        }

        @Override
        public void close() throws Exception {
            if (redisClient != null) {
                redisClient.close();
            }
            logger.info("RedisSinkWriter closed");
        }
    }
}
