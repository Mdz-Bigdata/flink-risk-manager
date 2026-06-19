package com.risk.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risk.event.LoginEvent;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;

/**
 * 登录事件 Kafka 反序列化器
 */
public class LoginEventDeserializer implements DeserializationSchema<LoginEvent> {

    private static final long serialVersionUID = 1L;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public LoginEvent deserialize(byte[] message) throws IOException {
        return objectMapper.readValue(message, LoginEvent.class);
    }

    @Override
    public boolean isEndOfStream(LoginEvent nextElement) {
        return false;
    }

    @Override
    public TypeInformation<LoginEvent> getProducedType() {
        return TypeInformation.of(LoginEvent.class);
    }
}
