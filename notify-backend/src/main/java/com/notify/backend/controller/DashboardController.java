package com.notify.backend.controller;

import com.notify.backend.dto.dashboard.ConsumerLagEntry;
import com.notify.backend.dto.dashboard.DashboardMetricsResponse;
import com.notify.backend.dto.dashboard.MetricsFilterRequest;
import com.notify.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Aggregate metrics and Kafka consumer lag")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/api/v1/dashboard/metrics")
    @Operation(summary = "Aggregated notification counts — filtered or unfiltered (unfiltered is cached 30s)")
    public DashboardMetricsResponse getMetrics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<Long> campaignIds,
            @RequestParam(required = false) String uploadFileName) {
        log.debug("GET /api/v1/dashboard/metrics");

        MetricsFilterRequest filter = MetricsFilterRequest.builder()
                .startDate(startDate != null ? Instant.parse(startDate) : null)
                .endDate(endDate != null ? Instant.parse(endDate) : null)
                .campaignIds(campaignIds)
                .uploadFileName(uploadFileName)
                .build();

        return dashboardService.getMetrics(filter);
    }

    @GetMapping("/api/v1/consumer/lag")
    @Operation(summary = "Per-partition consumer lag for all notification consumer groups")
    public List<ConsumerLagEntry> getConsumerLag() {
        log.debug("GET /api/v1/consumer/lag");
        return dashboardService.getConsumerLag();
    }
}