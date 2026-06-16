package com.notify.backend.service.impl;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import com.notify.backend.dto.dlq.DlqEventResponse;
import com.notify.backend.dto.notification.NotificationEventMessage;
import com.notify.backend.entity.DlqEvent;
import com.notify.backend.exception.DlqEventNotFoundException;
import com.notify.backend.kafka.producer.NotificationProducer;
import com.notify.backend.repository.DlqEventRepository;
import com.notify.backend.service.DlqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlqServiceImpl implements DlqService {

    private final DlqEventRepository dlqEventRepository;
    private final NotificationProducer producer;
    private final JsonMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<DlqEventResponse> listPending(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return dlqEventRepository.findByReplayedAtIsNull(pageable)
                .map(DlqEventResponse::from);
    }

    @Override
    @Transactional
    public DlqEventResponse reprocess(Long dlqEventId) {
        DlqEvent dlq = dlqEventRepository.findById(dlqEventId)
                .orElseThrow(() -> new DlqEventNotFoundException("DLQ event not found: " + dlqEventId));

        if (dlq.getReplayedAt() != null) {
            throw new IllegalStateException("DLQ event " + dlqEventId + " has already been replayed");
        }

        NotificationEventMessage message = deserializePayload(dlq.getPayload(), dlqEventId);
        message.setRetryCount(0);

        producer.sendToChannel(message);
        log.info("Replayed DLQ event: dlqId={}, eventId={}, channel={}", dlqEventId, message.getEventId(), message.getChannel());

        dlq.setReplayedAt(Instant.now());
        dlqEventRepository.save(dlq);

        return DlqEventResponse.from(dlq);
    }

    private NotificationEventMessage deserializePayload(String payload, Long dlqEventId) {
        try {
            return objectMapper.readValue(payload, NotificationEventMessage.class);
        } catch (JacksonException e) {
            log.error("Failed to deserialize DLQ payload for dlqId={}", dlqEventId, e);
            throw new IllegalStateException("Cannot replay DLQ event " + dlqEventId + ": malformed payload");
        }
    }
}