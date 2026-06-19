package com.risk.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risk.redis.RiskResult;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * Kafka Sink 工厂
 * 将 RiskResult 序列化为 JSON 后发送到 Kafka topic
 *
 * 下游消费者（如 Web 后端、告警系统、数据仓库）可订阅此 topic 获取实时风险结果
 */
public class KafkaSinkFactory {

    private static final String RISK_RESULT_TOPIC = "risk-results";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建风险结果 KafkaSink
     *
     * @param bootstrapServers Kafka 集群地址
     * @return KafkaSink<RiskResult>
     */
    public static KafkaSink<RiskResult> createRiskResultSink(String bootstrapServers) {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, "3");
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        producerProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        producerProps.put(ProducerConfig.BATCH_SIZE_CONFIG, "16384");
        producerProps.put(ProducerConfig.LINGER_MS_CONFIG, "5");

        return KafkaSink.<RiskResult>builder()
                .setBootstrapServers(bootstrapServers)
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.<RiskResult>builder()
                                .setTopic(RISK_RESULT_TOPIC)
                                .setKeySerializer(new StringSerializer())
                                .setValueSerializer(new RiskResultSerializer())
                                .build()
                )
                .setKafkaProducerConfig(producerProps)
                .build();
    }

    /**
     * RiskResult JSON 序列化器
     */
    public static class RiskResultSerializer implements org.apache.kafka.common.serialization.Serializer<RiskResult> {

        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public byte[] serialize(String topic, RiskResult data) {
            try {
                return mapper.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize RiskResult", e);
            }
        }

        @Override
        public void close() {
            // no-op
        }
    }
}
