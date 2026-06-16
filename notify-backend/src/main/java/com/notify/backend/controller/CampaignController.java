package com.notify.backend.controller;

import com.notify.backend.dto.campaign.CampaignResponse;
import com.notify.backend.dto.campaign.CreateCampaignRequest;
import com.notify.backend.dto.campaign.UpdateCampaignRequest;
import com.notify.backend.filter.UuidAuthFilter;
import com.notify.backend.service.NotificationCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
@Tag(name = "Campaigns", description = "Create and manage notification campaigns")
public class CampaignController {

    private final NotificationCampaignService campaignService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new campaign")
    public CampaignResponse create(
            @Valid @RequestBody CreateCampaignRequest request,
            HttpServletRequest httpRequest) {

        UUID clientId = extractClientId(httpRequest);
        log.info("POST /campaigns — clientId={}, channel={}", clientId, request.getChannel());
        return campaignService.createCampaign(request, clientId);
    }

    @GetMapping
    @Operation(summary = "List all campaigns for the authenticated client (paginated)")
    public Page<CampaignResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        UUID clientId = extractClientId(httpRequest);
        log.debug("GET /campaigns — clientId={}, page={}, size={}", clientId, page, size);
        return campaignService.getCampaigns(clientId, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single campaign by ID")
    public CampaignResponse getById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        UUID clientId = extractClientId(httpRequest);
        log.debug("GET /campaigns/{} — clientId={}", id, clientId);
        return campaignService.getCampaignById(id, clientId);
    }

    @PutMapping("/{id}")
    @Operation(summary =  "Update a single campaign by ID")
    public CampaignResponse updateById(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            @Valid @RequestBody UpdateCampaignRequest request
    ){

        UUID clientId = extractClientId(httpRequest);
        log.debug("PUT /campaigns/{} = clientId={}", id, clientId);
        return campaignService.updateCampaignById(id, clientId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel a campaign (soft delete — sets status to CANCELLED)")
    public void cancel(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        UUID clientId = extractClientId(httpRequest);
        log.info("DELETE /campaigns/{} — clientId={}", id, clientId);
        campaignService.cancelCampaign(id, clientId);
    }

    // Reads the clientId UUID placed in the request by UuidAuthFilter
    private UUID extractClientId(HttpServletRequest request) {
        return (UUID) request.getAttribute(UuidAuthFilter.CLIENT_ID_ATTR);
    }
}