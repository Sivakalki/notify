package com.notify.backend.service;

import com.notify.backend.dto.campaign.CampaignResponse;
import com.notify.backend.dto.campaign.CreateCampaignRequest;
import com.notify.backend.dto.campaign.UpdateCampaignRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface NotificationCampaignService {

    CampaignResponse createCampaign(CreateCampaignRequest request, UUID clientId);

    Page<CampaignResponse> getCampaigns(UUID clientId, int page, int size);

    CampaignResponse getCampaignById(Long id, UUID clientId);

    CampaignResponse updateCampaignById(long id, UUID clientId, UpdateCampaignRequest request);

    void cancelCampaign(Long id, UUID clientId);
}