package com.notify.backend.service.impl;

import com.notify.backend.dto.notification.*;
import com.notify.backend.entity.*;
import com.notify.backend.exception.CampaignNotFoundException;
import com.notify.backend.exception.DuplicateRequestException;
import com.notify.backend.kafka.producer.NotificationProducer;
import com.notify.backend.repository.*;
import com.notify.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationCampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final NotificationEventRepository eventRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationProducer producer;
    private final StringRedisTemplate redisTemplate;

    @Value("${notify.deduplication.idempotency-ttl-hours:24}")
    private long idempotencyTtlHours;

    @Override
    @Transactional
    public SendNotificationResponse send(SendNotificationRequest request, UUID clientId) {
        log.info("Notification send: campaignId={}, userId={}, clientId={}",
                request.getCampaignId(), request.getExternalUserId(), clientId);

        // 1. Validate campaign ownership
        NotificationCampaign campaign = campaignRepository
                .findByIdAndClientId(request.getCampaignId(), clientId)
                .orElseThrow(() -> new CampaignNotFoundException(
                        "Campaign not found: " + request.getCampaignId()));

        // 2. Build idempotency key — same campaign + same user = same notification
        String idempotencyKey = buildIdempotencyKey(campaign.getId(), request.getExternalUserId());
        String redisKey = "idempotency:" + idempotencyKey;

        // 3. Idempotency check — return early if already processed
        String existingEventId = redisTemplate.opsForValue().get(redisKey);
        if (existingEventId != null) {
            log.warn("Duplicate notification request detected: key={}", idempotencyKey);
            return SendNotificationResponse.builder()
                    .eventId(Long.valueOf(existingEventId))
                    .idempotencyKey(idempotencyKey)
                    .status(DeliveryStatus.PENDING)
                    .duplicate(true)
                    .build();
        }

        // 4. Upsert user — create if first time, reuse existing record otherwise
        User user = userRepository.findByExternalUserId(request.getExternalUserId())
                .orElseGet(() -> userRepository.save(User.builder()
                        .externalUserId(request.getExternalUserId())
                        .email(request.getEmail())
                        .phone(request.getPhone())
                        .build()));

        // 5. Persist the notification event (PENDING)
        NotificationEvent event = NotificationEvent.builder()
                .campaign(campaign)
                .user(user)
                .channel(campaign.getChannel())
                .idempotencyKey(idempotencyKey)
                .build();
        NotificationEvent saved = eventRepository.save(event);

        // 6. Create the delivery record (starts as PENDING)
        deliveryRepository.save(NotificationDelivery.builder()
                .event(saved)
                .channel(campaign.getChannel())
                .status(DeliveryStatus.PENDING)
                .build());

        // 7. Build the Kafka message with everything the consumer needs — no DB lookup required
        NotificationEventMessage message = NotificationEventMessage.builder()
                .eventId(saved.getId())
                .campaignId(campaign.getId())
                .userId(user.getId())
                .externalUserId(user.getExternalUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .channel(campaign.getChannel())
                .message(campaign.getMessage())
                .retryCount(0)
                .idempotencyKey(idempotencyKey)
                .build();

        // 8. Route to the correct channel topic
        producer.sendToChannel(message);

        // 9. Cache idempotency key so duplicate requests are caught before hitting the DB
        redisTemplate.opsForValue().set(redisKey, String.valueOf(saved.getId()),
                Duration.ofHours(idempotencyTtlHours));

        // 10. Increment campaign total user count atomically
        campaignRepository.addTotalUsers(campaign.getId(), 1);

        log.info("Notification event created: eventId={}, channel={}", saved.getId(), campaign.getChannel());

        return SendNotificationResponse.builder()
                .eventId(saved.getId())
                .idempotencyKey(idempotencyKey)
                .status(DeliveryStatus.PENDING)
                .duplicate(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationEventResponse> getHistory(Long campaignId, UUID clientId, int page, int size) {
        log.debug("Fetching notification history: campaignId={}, clientId={}", campaignId, clientId);

        // Validate campaign ownership before exposing its events
        campaignRepository.findByIdAndClientId(campaignId, clientId)
                .orElseThrow(() -> new CampaignNotFoundException("Campaign not found: " + campaignId));

        return eventRepository
                .findByCampaignId(campaignId, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toEventResponse);
    }

    private NotificationEventResponse toEventResponse(NotificationEvent event) {
        // Fetch delivery details without throwing if not yet created
        return deliveryRepository.findByEventId(event.getId())
                .map(delivery -> NotificationEventResponse.builder()
                        .eventId(event.getId())
                        .externalUserId(event.getUser().getExternalUserId())
                        .channel(event.getChannel())
                        .status(event.getStatus())
                        .createdAt(event.getCreatedAt())
                        .deliveredAt(delivery.getDeliveredAt())
                        .errorMessage(delivery.getErrorMessage())
                        .build())
                .orElseGet(() -> NotificationEventResponse.builder()
                        .eventId(event.getId())
                        .externalUserId(event.getUser().getExternalUserId())
                        .channel(event.getChannel())
                        .status(event.getStatus())
                        .createdAt(event.getCreatedAt())
                        .build());
    }

    private String buildIdempotencyKey(Long campaignId, String externalUserId) {
        return "notif:" + campaignId + ":" + externalUserId;
    }
}