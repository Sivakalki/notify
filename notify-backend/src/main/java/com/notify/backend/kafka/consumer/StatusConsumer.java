package com.notify.backend.kafka.consumer;

import com.notify.backend.dto.notification.NotificationStatusMessage;
import com.notify.backend.entity.*;
import com.notify.backend.kafka.KafkaTopics;
import com.notify.backend.metrics.NotificationMetrics;
import com.notify.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatusConsumer {

    private final NotificationEventRepository eventRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final DlqEventRepository dlqEventRepository;
    private final NotificationMetrics notificationMetrics;

    @KafkaListener(
            topics = KafkaTopics.NOTIFICATION_STATUS,
            groupId = "notify-status-group"
    )
    @Transactional
    public void consume(NotificationStatusMessage message) {
        log.info("StatusConsumer: eventId={}, status={}", message.getEventId(), message.getStatus());

        switch (message.getStatus()) {
            case SENT -> handleSent(message);
            case DLQ  -> handleDlq(message);
            default   -> log.warn("Unexpected status received: {}", message.getStatus());
        }
    }

    private void handleSent(NotificationStatusMessage message) {
        deliveryRepository.updateByEventId(message.getEventId(), DeliveryStatus.SENT, message.getDeliveredAt(), null);
        eventRepository.updateStatus(message.getEventId(), DeliveryStatus.SENT);
        notificationMetrics.recordSent(message.getChannel());
        log.info("Event {} marked SENT", message.getEventId());
    }

    private void handleDlq(NotificationStatusMessage message) {
        deliveryRepository.updateByEventId(message.getEventId(), DeliveryStatus.DLQ, null, message.getErrorMessage());
        eventRepository.updateStatus(message.getEventId(), DeliveryStatus.DLQ);
        dlqEventRepository.save(DlqEvent.builder()
                .event(eventRepository.getReferenceById(message.getEventId()))
                .reason(message.getErrorMessage())
                .payload(message.getDlqPayload())
                .build());
        notificationMetrics.recordDlq(message.getChannel());
        log.warn("Event {} moved to DLQ after {} retries", message.getEventId(), message.getRetryCount());
    }
}