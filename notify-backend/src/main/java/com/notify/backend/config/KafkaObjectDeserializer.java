package com.notify.backend.config;

import tools.jackson.databind.json.JsonMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.StandardCharsets;
import java.util.Set;

class KafkaObjectDeserializer implements Deserializer<Object> {

    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    private final Set<String> trustedPackages;

    KafkaObjectDeserializer(Set<String> trustedPackages) {
        this.trustedPackages = trustedPackages;
    }

    @Override
    public Object deserialize(String topic, byte[] data) {
        if (data == null) return null;
        try {
            return MAPPER.readValue(data, Object.class);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize JSON", e);
        }
    }

    @Override
    public Object deserialize(String topic, Headers headers, byte[] data) {
        if (data == null) return null;
        Header typeHeader = headers.lastHeader("__TypeId__");
        if (typeHeader != null) {
            String className = new String(typeHeader.value(), StandardCharsets.UTF_8);
            if (isTrusted(className)) {
                try {
                    return MAPPER.readValue(data, Class.forName(className));
                } catch (ClassNotFoundException e) {
                    throw new SerializationException("Untrusted or unknown type: " + className, e);
                } catch (Exception e) {
                    throw new SerializationException("Failed to deserialize JSON to " + className, e);
                }
            }
        }
        try {
            return MAPPER.readValue(data, Object.class);
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize JSON", e);
        }
    }

    private boolean isTrusted(String className) {
        return trustedPackages.contains("*")
                || trustedPackages.stream().anyMatch(className::startsWith);
    }
}