package com.notify.backend.dto.notification;

import com.notify.backend.entity.ChannelType;
import lombok.*;

/**
 * Kafka message payload published to notification-requested and channel-specific topics.
 * Carries everything a channel consumer needs to attempt delivery without a DB lookup.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventMessage {

    private Long eventId;
    private Long campaignId;
    private Long userId;
    private String externalUserId;
    private String email;
    private String phone;
    private ChannelType channel;
    private String message;

    // Incremented each time this message is re-published to retry-notification
    @Builder.Default
    private int retryCount = 0;

    // Used to prevent duplicate processing if the same message appears twice
    private String idempotencyKey;
}