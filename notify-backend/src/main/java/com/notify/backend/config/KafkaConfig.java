package com.notify.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    @Value("${notify.kafka.consumer.concurrency:3}")
    private int concurrency;

    // ── Producer ──────────────────────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Strongest delivery guarantee — no message loss even on broker failure
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        // Micro-batching — improves throughput during bulk upload without hurting latency much
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32_768);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new KafkaObjectSerializer());
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ── Consumer ──────────────────────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Manual offset commit via AckMode.RECORD — offsets only advance after successful processing
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // Controls how many records are fetched per poll — keeps memory usage bounded
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        // Must exceed the maximum retry backoff (3^3 = 27s) so the broker doesn't
        // evict the retry consumer while it is sleeping between delivery attempts
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 120_000); // 2 minutes
        var deserializer = new KafkaObjectDeserializer(Set.of("com.notify.backend.dto.notification"));
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // Commit offset after each record returns — ensures no message is skipped on restart
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        // Number of threads per listener — matches partition count for full parallelism
        factory.setConcurrency(concurrency);
        factory.setCommonErrorHandler(kafkaErrorHandler());
        return factory;
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Bean
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    // ── Error Handler ─────────────────────────────────────────────────────────

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        // Zero framework-level retries — all retry logic lives in RetryConsumer
        // FixedBackOff(interval=0ms, maxAttempts=0) means: fail fast, call recoverer immediately
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "Unrecoverable Kafka error — skipping record: topic={}, partition={}, offset={}, error={}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage(), exception),
                new FixedBackOff(0L, 0L)
        );
        // Deserialization failures are not retryable — bad bytes will never become good bytes
        handler.addNotRetryableExceptions(
                org.apache.kafka.common.errors.SerializationException.class,
                org.springframework.kafka.support.serializer.DeserializationException.class
        );
        return handler;
    }
}