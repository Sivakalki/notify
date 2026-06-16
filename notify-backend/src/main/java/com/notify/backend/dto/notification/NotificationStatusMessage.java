package com.notify.backend.dto.notification;

import com.notify.backend.entity.ChannelType;
import com.notify.backend.entity.DeliveryStatus;
import lombok.*;

import java.time.Instant;

/**
 * Kafka message payload published to notification-status after each delivery attempt.
 * StatusConsumer reads this and updates notification_delivery + notification_events tables.
 * When status=DLQ, dlqPayload carries the original NotificationEventMessage JSON
 * so it can be stored in dlq_events and replayed later.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatusMessage {

    private Long eventId;
    private Long campaignId;
    private ChannelType channel;
    private DeliveryStatus status;
    private Instant deliveredAt;
    private String errorMessage;
    private int retryCount;

    // Populated only when status=DLQ — stored as-is in dlq_events.payload for replay
    private String dlqPayload;
}