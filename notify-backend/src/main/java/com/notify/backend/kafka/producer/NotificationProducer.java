package com.notify.backend.kafka.producer;

import com.notify.backend.dto.notification.NotificationEventMessage;
import com.notify.backend.dto.notification.NotificationStatusMessage;
import com.notify.backend.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Routes a notification directly to the channel-specific topic.
     * Partition key is userId — guarantees ordering per user across retries.
     */
    public void sendToChannel(NotificationEventMessage message) {
        String topic = switch (message.getChannel()) {
            case EMAIL  -> KafkaTopics.EMAIL_NOTIFICATION;
            case SMS    -> KafkaTopics.SMS_NOTIFICATION;
            case IN_APP -> KafkaTopics.IN_APP_NOTIFICATION;
        };
        String key = String.valueOf(message.getUserId());
        send(topic, key, message);
        log.info("Published to topic={}, eventId={}, channel={}, userId={}",
                topic, message.getEventId(), message.getChannel(), message.getUserId());
    }

    /**
     * Publishes a failed message to the retry topic.
     * Partition key is eventId — keeps all retries for one event on the same partition.
     */
    public void sendToRetry(NotificationEventMessage message) {
        String key = String.valueOf(message.getEventId());
        send(KafkaTopics.RETRY_NOTIFICATION, key, message);
        log.warn("Published to retry topic: eventId={}, retryCount={}",
                message.getEventId(), message.getRetryCount());
    }

    /**
     * Publishes a delivery outcome (SENT or DLQ) to notification-status.
     * StatusConsumer is the single writer to the delivery tables.
     */
    public void sendStatus(NotificationStatusMessage message) {
        String key = String.valueOf(message.getEventId());
        send(KafkaTopics.NOTIFICATION_STATUS, key, message);
        log.debug("Published status={} for eventId={}", message.getStatus(), message.getEventId());
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void send(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish to topic={}, key={}: {}",
                                topic, key, ex.getMessage(), ex);
                    } else {
                        log.debug("Ack from topic={}, partition={}, offset={}",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}