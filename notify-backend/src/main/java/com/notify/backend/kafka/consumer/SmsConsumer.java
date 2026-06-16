package com.notify.backend.kafka.consumer;

import com.notify.backend.dto.notification.NotificationEventMessage;
import com.notify.backend.dto.notification.NotificationStatusMessage;
import com.notify.backend.entity.DeliveryStatus;
import com.notify.backend.kafka.KafkaTopics;
import com.notify.backend.kafka.producer.NotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsConsumer {

    private final NotificationProducer producer;

    @KafkaListener(
            topics = KafkaTopics.SMS_NOTIFICATION,
            groupId = "notify-sms-group"
    )
    public void consume(NotificationEventMessage message) {
        log.info("SmsConsumer: eventId={}, to={}, attempt={}",
                message.getEventId(), message.getPhone(), message.getRetryCount() + 1);

        boolean success = simulateSmsDelivery(message);

        if (success) {
            producer.sendStatus(NotificationStatusMessage.builder()
                    .eventId(message.getEventId())
                    .campaignId(message.getCampaignId())
                    .channel(message.getChannel())
                    .status(DeliveryStatus.SENT)
                    .deliveredAt(Instant.now())
                    .retryCount(message.getRetryCount())
                    .build());
            log.info("SMS delivered: eventId={}", message.getEventId());
        } else {
            producer.sendToRetry(message);
            log.warn("SMS failed, queued for retry: eventId={}", message.getEventId());
        }
    }

    private boolean simulateSmsDelivery(NotificationEventMessage message) {
        log.debug("Simulating SMS delivery to {}", message.getPhone());
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Math.random() > 0.2;
    }
}