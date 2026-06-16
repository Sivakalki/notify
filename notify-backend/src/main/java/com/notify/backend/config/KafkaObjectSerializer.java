package com.notify.backend.config;

import tools.jackson.databind.json.JsonMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

class KafkaObjectSerializer implements Serializer<Object> {

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    @Override
    public byte[] serialize(String topic, Object data) {
        return serialize(topic, null, data);
    }

    @Override
    public byte[] serialize(String topic, Headers headers, Object data) {
        if (data == null) return null;
        try {
            if (headers != null) {
                headers.add("__TypeId__", data.getClass().getName().getBytes(StandardCharsets.UTF_8));
            }
            return MAPPER.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize object to JSON", e);
        }
    }
}