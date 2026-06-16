package com.notify.backend.dto.campaign;

import com.notify.backend.dto.upload.BulkUploadResponse;
import com.notify.backend.entity.CampaignStatus;
import com.notify.backend.entity.ChannelType;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {

    private Long id;
    private String campaignName;
    private String message;
    private ChannelType channel;
    private CampaignStatus status;
    private Integer totalUsers;
    private Integer sentCount;
    private Integer failedCount;
    private Integer duplicateCount;
    private Instant createdAt;
    private Instant updatedAt;
    private List<BulkUploadResponse> uploads;
}