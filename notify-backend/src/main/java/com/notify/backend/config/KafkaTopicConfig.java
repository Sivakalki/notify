package com.notify.backend.config;

import com.notify.backend.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${notify.kafka.partitions.main:6}")
    private int mainPartitions;

    @Value("${notify.kafka.partitions.retry:3}")
    private int retryPartitions;

    @Value("${notify.kafka.replication-factor:1}")
    private int replicationFactor;

    // ── Channel topics (6 partitions each — one thread per partition) ─────────

    @Bean
    public NewTopic emailNotificationTopic() {
        return TopicBuilder.name(KafkaTopics.EMAIL_NOTIFICATION)
                .partitions(mainPartitions)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    public NewTopic smsNotificationTopic() {
        return TopicBuilder.name(KafkaTopics.SMS_NOTIFICATION)
                .partitions(mainPartitions)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    public NewTopic inAppNotificationTopic() {
        return TopicBuilder.name(KafkaTopics.IN_APP_NOTIFICATION)
                .partitions(mainPartitions)
                .replicas(replicationFactor)
                .build();
    }

    // ── Status topic (6 partitions — high volume, all channel consumers write here) ──

    @Bean
    public NewTopic notificationStatusTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_STATUS)
                .partitions(mainPartitions)
                .replicas(replicationFactor)
                .build();
    }

    // ── Retry topic (3 partitions — lower volume than main) ──────────────────

    @Bean
    public NewTopic retryNotificationTopic() {
        return TopicBuilder.name(KafkaTopics.RETRY_NOTIFICATION)
                .partitions(retryPartitions)
                .replicas(replicationFactor)
                .build();
    }
}