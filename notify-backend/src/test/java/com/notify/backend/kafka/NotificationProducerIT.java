package com.notify.backend.kafka;

import com.notify.backend.TestcontainersConfiguration;
import com.notify.backend.dto.notification.NotificationEventMessage;
import com.notify.backend.entity.ChannelType;
import com.notify.backend.kafka.producer.NotificationProducer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
// Disable all @KafkaListener containers so test messages aren't side-effected by consumers
@TestPropertySource(properties = "spring.kafka.listener.auto-startup=false")
class NotificationProducerIT {

    @Autowired NotificationProducer producer;
    @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers;

    @Test
    void sendToChannel_email_publishesMessageToEmailTopic() {
        NotificationEventMessage msg = NotificationEventMessage.builder()
                .eventId(1L)
                .campaignId(10L)
                .userId(100L)
                .externalUserId("u1")
                .email("u1@example.com")
                .channel(ChannelType.EMAIL)
                .message("Test notification")
                .retryCount(0)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        producer.sendToChannel(msg);

        try (KafkaConsumer<String, String> consumer = rawConsumer("it-email-verify")) {
            consumer.subscribe(List.of(KafkaTopics.EMAIL_NOTIFICATION));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.isEmpty()).isFalse();
            assertThat(records.iterator().next().value()).contains("u1@example.com");
        }
    }

    @Test
    void sendToChannel_sms_publishesMessageToSmsTopic() {
        NotificationEventMessage msg = NotificationEventMessage.builder()
                .eventId(2L)
                .campaignId(10L)
                .userId(101L)
                .externalUserId("u2")
                .phone("555-9999")
                .channel(ChannelType.SMS)
                .message("SMS test")
                .retryCount(0)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        producer.sendToChannel(msg);

        try (KafkaConsumer<String, String> consumer = rawConsumer("it-sms-verify")) {
            consumer.subscribe(List.of(KafkaTopics.SMS_NOTIFICATION));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.isEmpty()).isFalse();
        }
    }

    @Test
    void sendToRetry_publishesMessageToRetryTopic() {
        NotificationEventMessage msg = NotificationEventMessage.builder()
                .eventId(3L)
                .campaignId(10L)
                .userId(102L)
                .externalUserId("u3")
                .channel(ChannelType.EMAIL)
                .message("Retry test")
                .retryCount(1)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        producer.sendToRetry(msg);

        try (KafkaConsumer<String, String> consumer = rawConsumer("it-retry-verify")) {
            consumer.subscribe(List.of(KafkaTopics.RETRY_NOTIFICATION));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));
            assertThat(records.isEmpty()).isFalse();
        }
    }

    private KafkaConsumer<String, String> rawConsumer(String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5");
        return new KafkaConsumer<>(props);
    }
}