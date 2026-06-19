package com.risk.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risk.event.ActivityEvent;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;

/**
 * 活动事件 Kafka 反序列化器
 */
public class ActivityEventDeserializer implements DeserializationSchema<ActivityEvent> {

    private static final long serialVersionUID = 1L;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ActivityEvent deserialize(byte[] message) throws IOException {
        return objectMapper.readValue(message, ActivityEvent.class);
    }

    @Override
    public boolean isEndOfStream(ActivityEvent nextElement) {
        return false;
    }

    @Override
    public TypeInformation<ActivityEvent> getProducedType() {
        return TypeInformation.of(ActivityEvent.class);
    }
}
