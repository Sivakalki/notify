package com.notify.backend.dto.notification;

import com.notify.backend.entity.DeliveryStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SendNotificationResponse {

    private Long eventId;
    private String idempotencyKey;
    private DeliveryStatus status;

    // True if this exact request was already processed — safe to ignore
    private boolean duplicate;
}