package com.risk.sink;

import com.risk.redis.RiskResult;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis Sink（Flink 2.x Sink 接口实现）
 * 将 RiskResult 写入 Redis，key 格式：risk:{eventType}:{userId}:{eventId}
 *
 * 使用方式：
 *   riskResults.sinkTo(new RedisSinkFunction(host, port, db));
 *
 * 注意：Flink 2.x 推荐使用 sinkTo() 替代已废弃的 addSink()
 */
public class RedisSinkFunction extends RichSinkFunction<RiskResult> {
    private static final Logger logger = LoggerFactory.getLogger(RedisSinkFunction.class);

    private final String redisHost;
    private final int redisPort;
    private final int redisDatabase;
    private final int timeout;

    private transient com.risk.redis.RedisClient redisClient;

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
    public void open(OpenContext openContext) throws Exception {
        super.open(openContext);
        this.redisClient = new com.risk.redis.RedisClient(redisHost, redisPort, redisDatabase, timeout);
        logger.info("RedisSink opened: {}:{}", redisHost, redisPort);
    }

    @Override
    public void invoke(RiskResult value, SinkFunction.Context context) throws Exception {
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
    public void close() throws Exception {
        if (redisClient != null) {
            redisClient.close();
        }
        logger.info("RedisSink closed");
    }
}
