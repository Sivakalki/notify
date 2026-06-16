package com.notify.backend.kafka.consumer;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import com.notify.backend.dto.notification.NotificationEventMessage;
import com.notify.backend.dto.notification.NotificationStatusMessage;
import com.notify.backend.entity.DeliveryStatus;
import com.notify.backend.entity.RetryEvent;
import com.notify.backend.kafka.KafkaTopics;
import com.notify.backend.kafka.producer.NotificationProducer;
import com.notify.backend.metrics.NotificationMetrics;
import com.notify.backend.repository.NotificationEventRepository;
import com.notify.backend.repository.RetryEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryConsumer {

    private final NotificationProducer producer;
    private final RetryEventRepository retryEventRepository;
    private final NotificationEventRepository eventRepository;
    private final JsonMapper objectMapper;
    private final NotificationMetrics notificationMetrics;

    @Value("${notify.retry.max-attempts:3}")
    private int maxRetries;

    @KafkaListener(
            topics = KafkaTopics.RETRY_NOTIFICATION,
            groupId = "notify-retry-group"
    )
    public void consume(NotificationEventMessage message) {
        int nextRetryCount = message.getRetryCount() + 1;

        log.info("RetryConsumer: eventId={}, nextAttempt={}/{}",
                message.getEventId(), nextRetryCount, maxRetries);

        if (nextRetryCount > maxRetries) {
            sendToDlq(message, nextRetryCount);
            return;
        }

        applyBackoff(nextRetryCount, message.getEventId());
        persistRetryState(message.getEventId(), nextRetryCount);

        // Rebuild message with updated retry count and re-route to the channel topic
        NotificationEventMessage retryMessage = buildRetryMessage(message, nextRetryCount);
        producer.sendToChannel(retryMessage);
        notificationMetrics.recordRetry(message.getChannel());

        log.info("Re-published to channel after backoff: eventId={}, retryCount={}",
                message.getEventId(), nextRetryCount);
    }

    private void sendToDlq(NotificationEventMessage message, int retryCount) {
        log.warn("Max retries ({}) exceeded for eventId={} — sending to DLQ", maxRetries, message.getEventId());

        String payload = serializePayload(message);

        producer.sendStatus(NotificationStatusMessage.builder()
                .eventId(message.getEventId())
                .campaignId(message.getCampaignId())
                .channel(message.getChannel())
                .status(DeliveryStatus.DLQ)
                .errorMessage("Max retries (" + maxRetries + ") exceeded")
                .dlqPayload(payload)
                .retryCount(retryCount)
                .build());
    }

    // Exponential backoff: 3^retryCount seconds (3s, 9s, 27s)
    // NOTE: Thread.sleep is acceptable in a sample project with low volume.
    // In production, use a delay topic or a scheduled poller to avoid
    // blocking the consumer thread beyond max.poll.interval.ms.
    private void applyBackoff(int retryCount, Long eventId) {
        long backoffSeconds = (long) Math.pow(3, retryCount);
        log.info("Waiting {}s before retry: eventId={}", backoffSeconds, eventId);
        try {
            Thread.sleep(backoffSeconds * 1_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Retry backoff interrupted for eventId={}", eventId);
        }
    }

    private void persistRetryState(Long eventId, int retryCount) {
        eventRepository.findById(eventId).ifPresent(event -> {
            RetryEvent retryEvent = retryEventRepository.findByEventId(eventId)
                    .orElseGet(() -> RetryEvent.builder().event(event).build());
            retryEvent.setRetryCount(retryCount);
            retryEvent.setBackoffSeconds((int) Math.pow(3, retryCount));
            retryEvent.setNextRetryAt(Instant.now().plusSeconds((long) Math.pow(3, retryCount)));
            retryEventRepository.save(retryEvent);
        });
    }

    private NotificationEventMessage buildRetryMessage(NotificationEventMessage original, int retryCount) {
        return NotificationEventMessage.builder()
                .eventId(original.getEventId())
                .campaignId(original.getCampaignId())
                .userId(original.getUserId())
                .externalUserId(original.getExternalUserId())
                .email(original.getEmail())
                .phone(original.getPhone())
                .channel(original.getChannel())
                .message(original.getMessage())
                .retryCount(retryCount)
                .idempotencyKey(original.getIdempotencyKey())
                .build();
    }

    private String serializePayload(NotificationEventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JacksonException e) {
            log.error("Failed to serialize DLQ payload for eventId={}", message.getEventId());
            return "{}";
        }
    }
}