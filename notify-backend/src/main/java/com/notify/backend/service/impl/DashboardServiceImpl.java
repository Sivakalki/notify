package com.notify.backend.service.impl;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import com.notify.backend.dto.dashboard.ConsumerLagEntry;
import com.notify.backend.dto.dashboard.DashboardMetricsResponse;
import com.notify.backend.dto.dashboard.MetricsFilterRequest;
import com.notify.backend.entity.DeliveryStatus;
import com.notify.backend.repository.NotificationCampaignRepository;
import com.notify.backend.repository.NotificationEventRepository;
import com.notify.backend.repository.UploadedFileRepository;
import com.notify.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.common.TopicPartition;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final String CACHE_KEY   = "dashboard:metrics";
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private static final Instant FAR_PAST   = Instant.EPOCH;
    private static final Instant FAR_FUTURE = Instant.parse("9999-12-31T23:59:59Z");

    private static final List<String> CONSUMER_GROUPS = List.of(
            "notify-email-group",
            "notify-sms-group",
            "notify-inapp-group",
            "notify-status-group",
            "notify-retry-group"
    );

    private final NotificationCampaignRepository campaignRepository;
    private final NotificationEventRepository eventRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final StringRedisTemplate redisTemplate;
    private final AdminClient adminClient;
    private final JsonMapper objectMapper;

    @Override
    public DashboardMetricsResponse getMetrics(MetricsFilterRequest filter) {
        if (!filter.hasFilter()) {
            return getCachedOrFresh();
        }
        return computeFiltered(filter);
    }

    private DashboardMetricsResponse getCachedOrFresh() {
        String cached = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, DashboardMetricsResponse.class);
            } catch (JacksonException e) {
                log.warn("Failed to deserialize cached metrics — recomputing", e);
            }
        }

        Map<DeliveryStatus, Long> statusCounts = eventRepository.countGroupedByStatus()
                .stream()
                .collect(Collectors.toMap(row -> (DeliveryStatus) row[0], row -> (Long) row[1]));

        DashboardMetricsResponse metrics = DashboardMetricsResponse.builder()
                .totalSent(statusCounts.getOrDefault(DeliveryStatus.SENT, 0L))
                .totalPending(statusCounts.getOrDefault(DeliveryStatus.PENDING, 0L))
                .totalFailed(statusCounts.getOrDefault(DeliveryStatus.DLQ, 0L))
                .totalDuplicates(uploadedFileRepository.sumDuplicateCount())
                .totalCampaigns(campaignRepository.count())
                .cachedAt(Instant.now())
                .build();

        try {
            redisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(metrics), CACHE_TTL);
        } catch (JacksonException e) {
            log.warn("Failed to cache dashboard metrics", e);
        }

        return metrics;
    }

    private DashboardMetricsResponse computeFiltered(MetricsFilterRequest filter) {
        Instant startDate = filter.getStartDate() != null ? filter.getStartDate() : FAR_PAST;
        Instant endDate   = filter.getEndDate()   != null ? filter.getEndDate()   : FAR_FUTURE;

        List<Long> campaignIds = filter.hasCampaignFilter() ? resolveCampaignIds(filter) : null;

        // Filters matched nothing → return zeros
        if (campaignIds != null && campaignIds.isEmpty()) {
            return emptyResponse();
        }

        List<Object[]> rows = (campaignIds != null)
                ? eventRepository.countGroupedByStatusByCampaigns(campaignIds, startDate, endDate)
                : eventRepository.countGroupedByStatusInDateRange(startDate, endDate);

        Map<DeliveryStatus, Long> statusCounts = rows.stream()
                .collect(Collectors.toMap(r -> (DeliveryStatus) r[0], r -> (Long) r[1]));

        long duplicates = (campaignIds != null)
                ? uploadedFileRepository.sumDuplicateCountByCampaignIdsInDateRange(campaignIds, startDate, endDate)
                : uploadedFileRepository.sumDuplicateCountInDateRange(startDate, endDate);

        long campaigns = (campaignIds != null) ? campaignIds.size() : campaignRepository.count();

        return DashboardMetricsResponse.builder()
                .totalSent(statusCounts.getOrDefault(DeliveryStatus.SENT, 0L))
                .totalPending(statusCounts.getOrDefault(DeliveryStatus.PENDING, 0L))
                .totalFailed(statusCounts.getOrDefault(DeliveryStatus.DLQ, 0L))
                .totalDuplicates(duplicates)
                .totalCampaigns(campaigns)
                .cachedAt(Instant.now())
                .build();
    }

    private List<Long> resolveCampaignIds(MetricsFilterRequest filter) {
        Set<Long> ids = null;

        if (filter.getCampaignIds() != null && !filter.getCampaignIds().isEmpty()) {
            ids = new HashSet<>(filter.getCampaignIds());
        }

        if (filter.getUploadFileName() != null && !filter.getUploadFileName().isBlank()) {
            List<Long> fileIds = uploadedFileRepository.findCampaignIdsByFileNameContaining(filter.getUploadFileName());
            if (ids == null) {
                ids = new HashSet<>(fileIds);
            } else {
                ids.retainAll(fileIds); // intersection: campaign must match both filters
            }
        }

        return ids == null ? List.of() : List.copyOf(ids);
    }

    private DashboardMetricsResponse emptyResponse() {
        return DashboardMetricsResponse.builder()
                .totalSent(0L).totalPending(0L).totalFailed(0L)
                .totalDuplicates(0L).totalCampaigns(0L)
                .cachedAt(Instant.now())
                .build();
    }

    @Override
    public List<ConsumerLagEntry> getConsumerLag() {
        List<ConsumerLagEntry> result = new ArrayList<>();
        try {
            for (String groupId : CONSUMER_GROUPS) {
                result.addAll(computeGroupLag(adminClient, groupId));
            }
        } catch (Exception e) {
            log.error("Failed to compute consumer lag", e);
        }
        return result;
    }

    private List<ConsumerLagEntry> computeGroupLag(AdminClient adminClient, String groupId) {
        try {
            Map<TopicPartition, Long> committedOffsets = adminClient
                    .listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata()
                    .get()
                    .entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().offset()));

            if (committedOffsets.isEmpty()) return List.of();

            Map<TopicPartition, OffsetSpec> latestSpec = new HashMap<>();
            committedOffsets.keySet().forEach(tp -> latestSpec.put(tp, OffsetSpec.latest()));

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endOffsets = adminClient
                    .listOffsets(latestSpec).all().get();

            List<ConsumerLagEntry> entries = new ArrayList<>();
            for (Map.Entry<TopicPartition, Long> committed : committedOffsets.entrySet()) {
                TopicPartition tp = committed.getKey();
                long endOffset = endOffsets.getOrDefault(tp,
                        new ListOffsetsResult.ListOffsetsResultInfo(0, -1, Optional.empty())).offset();
                long lag = Math.max(0, endOffset - committed.getValue());

                entries.add(ConsumerLagEntry.builder()
                        .groupId(groupId)
                        .topic(tp.topic())
                        .partition(tp.partition())
                        .lag(lag)
                        .build());
            }
            return entries;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while computing lag for group={}", groupId);
            return List.of();
        } catch (ExecutionException e) {
            log.warn("Failed to compute lag for group={}: {}", groupId, e.getMessage());
            return List.of();
        }
    }
}