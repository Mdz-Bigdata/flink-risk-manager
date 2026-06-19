package com.qinyadan.risk.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinyadan.risk.event.OrderEvent;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;

/**
 * 订单事件 Kafka 反序列化器
 */
public class OrderEventDeserializer implements DeserializationSchema<OrderEvent> {

    private static final long serialVersionUID = 1L;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OrderEvent deserialize(byte[] message) throws IOException {
        return objectMapper.readValue(message, OrderEvent.class);
    }

    @Override
    public boolean isEndOfStream(OrderEvent nextElement) {
        return false;
    }

    @Override
    public TypeInformation<OrderEvent> getProducedType() {
        return TypeInformation.of(OrderEvent.class);
    }
}
