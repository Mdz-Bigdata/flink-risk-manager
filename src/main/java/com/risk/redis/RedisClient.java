package com.risk.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Serializable;

/**
 * Redis 客户端
 */
public class RedisClient implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(RedisClient.class);

    private transient JedisPool jedisPool;
    private final String host;
    private final int port;
    private final int database;
    private final int timeout;

    public RedisClient(String host, int port, int database, int timeout) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.timeout = timeout;
        initPool();
    }

    private void initPool() {
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(10);
            config.setMaxIdle(5);
            config.setMinIdle(2);
            config.setTestOnBorrow(true);
            config.setTestOnReturn(true);
            
            this.jedisPool = new JedisPool(config, host, port, timeout);
            logger.info("Redis connection pool initialized: {}:{}", host, port);
        } catch (Exception e) {
            logger.error("Failed to initialize Redis connection pool", e);
            throw new RuntimeException("Redis pool initialization failed", e);
        }
    }

    /**
     * 设置键值对
     */
    public void set(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(database);
            jedis.set(key, value);
        } catch (Exception e) {
            logger.error("Error setting key: {}", key, e);
        }
    }

    /**
     * 设置键值对并指定过期时间（秒）
     */
    public void setWithTTL(String key, String value, long ttlSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(database);
            jedis.setex(key, ttlSeconds, value);
        } catch (Exception e) {
            logger.error("Error setting key with TTL: {}", key, e);
        }
    }

    /**
     * 获取值
     */
    public String get(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(database);
            return jedis.get(key);
        } catch (Exception e) {
            logger.error("Error getting key: {}", key, e);
            return null;
        }
    }

    /**
     * 删除键
     */
    public void delete(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(database);
            jedis.del(key);
        } catch (Exception e) {
            logger.error("Error deleting key: {}", key, e);
        }
    }

    /**
     * 检查键是否存在
     */
    public boolean exists(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(database);
            return jedis.exists(key);
        } catch (Exception e) {
            logger.error("Error checking key existence: {}", key, e);
            return false;
        }
    }

    /**
     * 获取键的过期时间（秒）
     */
    public long getTTL(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(database);
            return jedis.ttl(key);
        } catch (Exception e) {
            logger.error("Error getting TTL: {}", key, e);
            return -1;
        }
    }

    /**
     * 增加计数器
     */
    public void increment(String key, long delta) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(database);
            jedis.incrBy(key, delta);
        } catch (Exception e) {
            logger.error("Error incrementing key: {}", key, e);
        }
    }

    /**
     * 关闭连接池
     */
    public void close() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("Redis connection pool closed");
        }
    }
}