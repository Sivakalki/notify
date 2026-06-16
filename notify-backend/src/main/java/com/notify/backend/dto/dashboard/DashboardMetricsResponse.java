package com.notify.backend.dto.dashboard;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsResponse {

    private long totalSent;
    private long totalFailed;
    private long totalPending;
    private long totalDuplicates;
    private long totalCampaigns;
    private Instant cachedAt;
}