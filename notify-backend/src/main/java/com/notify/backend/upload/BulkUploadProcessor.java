package com.notify.backend.upload;

import com.notify.backend.dto.notification.NotificationEventMessage;
import com.notify.backend.dto.upload.UserRow;
import com.notify.backend.entity.*;
import com.notify.backend.kafka.producer.NotificationProducer;
import com.notify.backend.repository.*;
import com.notify.backend.service.DeduplicationService;
import com.notify.backend.upload.parser.FileParserFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BulkUploadProcessor {

    private final FileParserFactory parserFactory;
    private final DeduplicationService deduplicationService;
    private final UserRepository userRepository;
    private final NotificationEventRepository eventRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationCampaignRepository campaignRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final NotificationProducer producer;

    /**
     * Parses the file, deduplicates via Cuckoo Filter, persists events, and publishes to Kafka.
     * Runs in a dedicated thread pool so the HTTP response is returned immediately.
     *
     * One transaction wraps all rows — keeps the DB consistent with the upload status.
     * Trade-off: Kafka publishes inside the transaction are not rolled back if the DB rolls back.
     * For production, integrate KafkaTransactionManager with the JDBC transaction manager.
     */
    @Async("bulkUploadExecutor")
    @Transactional
    public void processAsync(Long uploadId, Long campaignId, byte[] fileBytes, FileType fileType) {
        log.info("Bulk upload started: uploadId={}, campaignId={}, type={}", uploadId, campaignId, fileType);

        UploadedFile upload = uploadedFileRepository.findById(uploadId)
                .orElseThrow(() -> new IllegalStateException("UploadedFile not found: " + uploadId));
        NotificationCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalStateException("Campaign not found: " + campaignId));

        try {
            List<UserRow> rows = parserFactory.getParser(fileType)
                    .parse(new ByteArrayInputStream(fileBytes));

            int newCount = 0;
            int dupCount = 0;
            int total    = rows.size();

            int skipCount = 0;
            for (int i = 0; i < total; i++) {
                UserRow row = rows.get(i);
                if ((row.getEmail() == null || row.getEmail().isBlank()) &&
                    (row.getPhone() == null || row.getPhone().isBlank())) {
                    log.warn("Skipping row {} — no email or phone: userId={}", i + 1, row.getExternalUserId());
                    skipCount++;
                    continue;
                }
                boolean isNew = deduplicationService.addIfAbsent(campaignId, row.getExternalUserId());
                if (isNew) {
                    publishRow(row, campaign);
                    newCount++;
                } else {
                    dupCount++;
                }
                if ((i + 1) % 1000 == 0 || (i + 1) == total) {
                    log.info("Bulk upload progress: uploadId={}, processed={}/{}, new={}, duplicates={}",
                            uploadId, i + 1, total, newCount, dupCount);
                }
            }

            // Batch-update counters once at the end — avoids N separate UPDATE queries
            if (newCount > 0) {
                campaignRepository.addTotalUsers(campaignId, newCount);
            }
            upload.setRowCount(rows.size());
            upload.setDuplicateCount(dupCount);
            upload.setStatus(FileStatus.COMPLETED);
            uploadedFileRepository.save(upload);

            log.info("Bulk upload completed: uploadId={}, total={}, new={}, duplicates={}, skipped={}",
                    uploadId, rows.size(), newCount, dupCount, skipCount);

        } catch (Exception e) {
            log.error("Bulk upload failed: uploadId={}", uploadId, e);
            upload.setStatus(FileStatus.FAILED);
            uploadedFileRepository.save(upload);
            // Do not rethrow — exception is caught so the transaction commits with FAILED status
        }
    }

    private void publishRow(UserRow row, NotificationCampaign campaign) {
        User user = userRepository.findByExternalUserId(row.getExternalUserId())
                .orElseGet(() -> userRepository.save(User.builder()
                        .externalUserId(row.getExternalUserId())
                        .email(row.getEmail())
                        .phone(row.getPhone())
                        .build()));

        String idempotencyKey = "notif:" + campaign.getId() + ":" + user.getExternalUserId();

        NotificationEvent event = eventRepository.save(NotificationEvent.builder()
                .campaign(campaign)
                .user(user)
                .channel(campaign.getChannel())
                .idempotencyKey(idempotencyKey)
                .build());

        deliveryRepository.save(NotificationDelivery.builder()
                .event(event)
                .channel(campaign.getChannel())
                .status(DeliveryStatus.PENDING)
                .build());

        producer.sendToChannel(NotificationEventMessage.builder()
                .eventId(event.getId())
                .campaignId(campaign.getId())
                .userId(user.getId())
                .externalUserId(user.getExternalUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .channel(campaign.getChannel())
                .message(campaign.getMessage())
                .retryCount(0)
                .idempotencyKey(idempotencyKey)
                .build());
    }
}
