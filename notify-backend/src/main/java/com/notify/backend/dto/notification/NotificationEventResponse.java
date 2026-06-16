package com.notify.backend.dto.notification;

import com.notify.backend.entity.ChannelType;
import com.notify.backend.entity.DeliveryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class NotificationEventResponse {

    private Long eventId;
    private String externalUserId;
    private ChannelType channel;
    private DeliveryStatus status;
    private Instant createdAt;

    // Delivery outcome — null if still pending or retrying
    private Instant deliveredAt;
    private String errorMessage;
}