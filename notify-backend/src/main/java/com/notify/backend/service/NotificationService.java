package com.notify.backend.service;

import com.notify.backend.dto.notification.NotificationEventResponse;
import com.notify.backend.dto.notification.SendNotificationRequest;
import com.notify.backend.dto.notification.SendNotificationResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface NotificationService {

    SendNotificationResponse send(SendNotificationRequest request, UUID clientId);

    Page<NotificationEventResponse> getHistory(Long campaignId, UUID clientId, int page, int size);
}