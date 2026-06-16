package com.notify.backend.dto.dashboard;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsFilterRequest {

    private Instant startDate;
    private Instant endDate;
    private List<Long> campaignIds;
    private String uploadFileName;

    public boolean hasFilter() {
        return startDate != null || endDate != null || hasCampaignFilter();
    }

    public boolean hasCampaignFilter() {
        return (campaignIds != null && !campaignIds.isEmpty()) ||
               (uploadFileName != null && !uploadFileName.isBlank());
    }
}