package com.notify.backend.service.impl;

import com.notify.backend.dto.campaign.CampaignResponse;
import com.notify.backend.dto.campaign.CreateCampaignRequest;
import com.notify.backend.dto.campaign.UpdateCampaignRequest;
import com.notify.backend.dto.upload.BulkUploadResponse;
import com.notify.backend.entity.CampaignStatus;
import com.notify.backend.entity.DeliveryStatus;
import com.notify.backend.entity.FrontendClient;
import com.notify.backend.entity.NotificationCampaign;
import com.notify.backend.entity.UploadedFile;
import com.notify.backend.exception.CampaignNotFoundException;
import com.notify.backend.repository.FrontendClientRepository;
import com.notify.backend.repository.NotificationCampaignRepository;
import com.notify.backend.repository.NotificationEventRepository;
import com.notify.backend.repository.UploadedFileRepository;
import com.notify.backend.service.NotificationCampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCampaignServiceImpl implements NotificationCampaignService {

    private final NotificationCampaignRepository campaignRepository;
    private final FrontendClientRepository clientRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final NotificationEventRepository eventRepository;

    @Override
    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request, UUID clientId) {
        log.info("Creating campaign: name='{}', channel={}, clientId={}",
                request.getCampaignName(), request.getChannel(), clientId);

        // getReferenceById returns a proxy — avoids a DB round-trip just to set the FK
        FrontendClient clientRef = clientRepository.getReferenceById(clientId);

        NotificationCampaign campaign = NotificationCampaign.builder()
                .client(clientRef)
                .campaignName(request.getCampaignName())
                .message(request.getMessage())
                .channel(request.getChannel())
                .build();

        NotificationCampaign saved = campaignRepository.save(campaign);
        log.info("Campaign created: id={}, status={}", saved.getId(), saved.getStatus());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CampaignResponse> getCampaigns(UUID clientId, int page, int size) {
        log.debug("Fetching campaigns for clientId={}, page={}, size={}", clientId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return campaignRepository.findByClientId(clientId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(Long id, UUID clientId) {
        log.debug("Fetching campaign id={} for clientId={}", id, clientId);

        NotificationCampaign campaign = campaignRepository
                .findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new CampaignNotFoundException(
                        "Campaign not found with id: " + id));

        List<BulkUploadResponse> uploads = uploadedFileRepository.findByCampaignId(id)
                .stream()
                .map(this::toUploadResponse)
                .toList();

        return toResponse(campaign, uploads);
    }

    @Override
    @Transactional
    public CampaignResponse updateCampaignById(long id, UUID clientId, UpdateCampaignRequest request){
        log.info("Updating campaign id={} for clientId={}", id, clientId);
        NotificationCampaign campaign = campaignRepository
                .findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new CampaignNotFoundException(
                        "Campaign not found with id: "+ id));
        campaign.setCampaignName(request.getCampaignName());
        campaign.setMessage(request.getMessage());
        campaign.setChannel(request.getChannel());
        campaign.setStatus(request.getStatus());

        NotificationCampaign updated = campaignRepository.save(campaign);

        return toResponse(updated);
    }

    @Override
    @Transactional
    public void cancelCampaign(Long id, UUID clientId) {
        log.info("Cancelling campaign id={} for clientId={}", id, clientId);

        NotificationCampaign campaign = campaignRepository
                .findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new CampaignNotFoundException(
                        "Campaign not found with id: " + id));

        if (campaign.getStatus() == CampaignStatus.CANCELLED) {
            log.warn("Campaign id={} is already cancelled", id);
            return;
        }

        campaign.setStatus(CampaignStatus.CANCELLED);
        campaignRepository.save(campaign);
        log.info("Campaign id={} cancelled successfully", id);
    }

    private CampaignResponse toResponse(NotificationCampaign campaign) {
        return toResponse(campaign, null);
    }

    private CampaignResponse toResponse(NotificationCampaign campaign, List<BulkUploadResponse> uploads) {
        return CampaignResponse.builder()
                .id(campaign.getId())
                .campaignName(campaign.getCampaignName())
                .message(campaign.getMessage())
                .channel(campaign.getChannel())
                .status(campaign.getStatus())
                .totalUsers(campaign.getTotalUsers())
                .sentCount((int) eventRepository.countByCampaignIdAndStatus(campaign.getId(), DeliveryStatus.SENT))
                .failedCount((int) eventRepository.countByCampaignIdAndStatus(campaign.getId(), DeliveryStatus.DLQ))
                .duplicateCount((int) uploadedFileRepository.sumDuplicateCountByCampaignId(campaign.getId()))
                .createdAt(campaign.getCreatedAt())
                .updatedAt(campaign.getUpdatedAt())
                .uploads(uploads)
                .build();
    }

    private BulkUploadResponse toUploadResponse(UploadedFile upload) {
        return BulkUploadResponse.builder()
                .uploadId(upload.getId())
                .campaignId(upload.getCampaign().getId())
                .fileName(upload.getFileName())
                .fileType(upload.getFileType())
                .rowCount(upload.getRowCount())
                .duplicateCount(upload.getDuplicateCount())
                .status(upload.getStatus())
                .createdAt(upload.getCreatedAt())
                .build();
    }
}