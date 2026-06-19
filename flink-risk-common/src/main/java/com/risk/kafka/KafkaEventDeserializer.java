package com.risk.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.risk.event.ActivityEvent;
import com.risk.event.Event;
import com.risk.event.LoginEvent;
import com.risk.event.OrderEvent;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;

/**
 * Kafka JSON 反序列化器 -> Event 对象
 *
 * JSON 格式示例 (LoginEvent):
 * {
 *   "userId": "user001",
 *   "eventId": "evt_001",
 *   "timestamp": 1710000000000,
 *   "eventType": "LOGIN",
 *   "ip": "192.168.1.1",
 *   "deviceId": "dev_001",
 *   "success": false
 * }
 */
public class KafkaEventDeserializer implements DeserializationSchema<Event> {

    private static final long serialVersionUID = 1L;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Event deserialize(byte[] message) throws IOException {
        JsonNode root = objectMapper.readTree(message);
        String eventType = root.has("eventType") ? root.get("eventType").asText() : "";

        // 根据 eventType 反序列化为对应的 Event 子类
        switch (eventType.toUpperCase()) {
            case "LOGIN":
                return objectMapper.readValue(message, LoginEvent.class);
            case "ORDER":
                return objectMapper.readValue(message, OrderEvent.class);
            case "ACTIVITY":
                return objectMapper.readValue(message, ActivityEvent.class);
            default:
                // 未知类型，返回通用 Event
                Event event = objectMapper.readValue(message, Event.class);
                return event;
        }
    }

    @Override
    public boolean isEndOfStream(Event nextElement) {
        return false;
    }

    @Override
    public TypeInformation<Event> getProducedType() {
        return TypeInformation.of(Event.class);
    }
}
