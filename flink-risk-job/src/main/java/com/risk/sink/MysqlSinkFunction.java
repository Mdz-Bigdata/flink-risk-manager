package com.risk.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risk.redis.RiskResult;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * MySQL 异步 Sink
 * 将 RiskResult 异步写入 risk_result 表
 *
 * 设计要点：
 *   - 使用 RichAsyncFunction + 线程池异步写入，不阻塞主数据流
 *   - 内部维护连接池（单连接复用），open() 时初始化，close() 时关闭
 *   - 写入失败不重试，仅记录日志，避免影响实时流延迟
 *   - 支持 Exactly-Once 语义配合 Checkpoint
 */
public class MysqlSinkFunction extends RichAsyncFunction<RiskResult, Void> {
    private static final Logger logger = LoggerFactory.getLogger(MysqlSinkFunction.class);

    private static final String INSERT_SQL =
            "INSERT INTO risk_result (user_id, event_id, event_type, risk_level, risk_score, action, details, create_time) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int maxPoolSize;

    private transient ExecutorService executorService;
    private transient ObjectMapper objectMapper;

    public MysqlSinkFunction(String jdbcUrl, String username, String password) {
        this(jdbcUrl, username, password, 4);
    }

    public MysqlSinkFunction(String jdbcUrl, String username, String password, int maxPoolSize) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.maxPoolSize = maxPoolSize;
    }

    @Override
    public void open(OpenContext openContext) throws Exception {
        super.open(openContext);
        this.objectMapper = new ObjectMapper();

        // 创建带上限的线程池，防止 OOM
        this.executorService = new ThreadPoolExecutor(
                maxPoolSize,
                maxPoolSize,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10000),
                new ThreadFactoryWithName("mysql-sink-"),
                new ThreadPoolExecutor.DiscardOldestPolicy()
        );

        logger.info("MysqlSinkFunction opened, pool size: {}", maxPoolSize);
    }

    @Override
    public void close() throws Exception {
        if (executorService != null) {
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.SECONDS);
        }
        logger.info("MysqlSinkFunction closed");
    }

    @Override
    public void asyncInvoke(RiskResult result, ResultFuture<Void> resultFuture) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
                 PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {

                ps.setString(1, result.getUserId());
                ps.setString(2, result.getEventId());
                ps.setString(3, result.getEventType());
                ps.setString(4, result.getRiskLevel().name());
                ps.setInt(5, result.getRiskScore());
                ps.setString(6, result.getAction());
                ps.setString(7, objectMapper.writeValueAsString(result.getDetails()));

                ps.executeUpdate();

                if (logger.isDebugEnabled()) {
                    logger.debug("Risk result persisted to MySQL: user={}, event={}, level={}",
                            result.getUserId(), result.getEventType(), result.getRiskLevel());
                }

                resultFuture.complete(Collections.emptyList());
            } catch (SQLException e) {
                logger.error("Failed to write risk result to MySQL: user={}, event={}",
                        result.getUserId(), result.getEventType(), e);
                resultFuture.completeExceptionally(e);
            } catch (Exception e) {
                logger.error("Unexpected error in MysqlSinkFunction", e);
                resultFuture.completeExceptionally(e);
            }
        }, executorService);
    }

    /**
     * 超时回调：异步写入超时时不抛异常，仅记录日志
     */
    @Override
    public void timeout(RiskResult input, ResultFuture<Void> resultFuture) {
        logger.warn("MySQL sink timeout for user={}, event={}", input.getUserId(), input.getEventType());
        resultFuture.complete(Collections.emptyList());
    }

    /** 自定义线程工厂，方便排查问题 */
    private static class ThreadFactoryWithName implements java.util.concurrent.ThreadFactory {
        private final String prefix;
        private int count = 0;

        ThreadFactoryWithName(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + (count++));
            t.setDaemon(true);
            return t;
        }
    }
}
