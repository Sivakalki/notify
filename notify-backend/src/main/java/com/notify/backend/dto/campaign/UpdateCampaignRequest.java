package com.notify.backend.dto.campaign;

import com.notify.backend.entity.CampaignStatus;
import com.notify.backend.entity.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCampaignRequest {

    @NotBlank(message = "Campaign name is required")
    @Size(min = 2, max = 255, message = "Campaign name must be between 2 and 255 characters")
    private String campaignName;

    @NotBlank(message = "Message is required")
    @Size(max = 5000, message = "Message cannot exceed 5000 characters")
    private String message;

    @NotNull(message = "Channel is required")
    private ChannelType channel;

    @NotNull(message = "Status is required")
    private CampaignStatus status;
}
