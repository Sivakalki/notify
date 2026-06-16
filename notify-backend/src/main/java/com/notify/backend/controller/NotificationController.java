package com.notify.backend.controller;

import com.notify.backend.dto.notification.NotificationEventResponse;
import com.notify.backend.dto.notification.SendNotificationRequest;
import com.notify.backend.dto.notification.SendNotificationResponse;
import com.notify.backend.dto.upload.BulkUploadResponse;
import com.notify.backend.filter.UuidAuthFilter;
import com.notify.backend.service.NotificationService;
import com.notify.backend.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Send notifications and view delivery history")
public class NotificationController {

    private final NotificationService notificationService;
    private final UploadService uploadService;

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Send a notification to a single user for a campaign")
    public SendNotificationResponse send(
            @Valid @RequestBody SendNotificationRequest request,
            HttpServletRequest httpRequest) {

        UUID clientId = extractClientId(httpRequest);
        log.info("POST /notifications/send — clientId={}, campaignId={}, userId={}",
                clientId, request.getCampaignId(), request.getExternalUserId());
        return notificationService.send(request, clientId);
    }

    @GetMapping("/history")
    @Operation(summary = "Get notification delivery history for a campaign (paginated)")
    public Page<NotificationEventResponse> history(
            @RequestParam Long campaignId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest httpRequest) {

        UUID clientId = extractClientId(httpRequest);
        log.debug("GET /notifications/history — clientId={}, campaignId={}", clientId, campaignId);
        return notificationService.getHistory(campaignId, clientId, page, size);
    }

    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Bulk send notifications from a CSV / Excel / JSON file",
               description = "Accepts .csv, .xlsx, .xls, .json. Required column: externalUserId. Optional: email, phone. Returns immediately; processing is async.")
    public BulkUploadResponse bulkUpload(
            @RequestParam("campaignId") Long campaignId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        UUID clientId = extractClientId(httpRequest);
        log.info("POST /notifications/bulk-upload — clientId={}, campaignId={}, file={}",
                clientId, campaignId, file.getOriginalFilename());
        return uploadService.bulkUpload(file, campaignId, clientId);
    }

    private UUID extractClientId(HttpServletRequest request) {
        return (UUID) request.getAttribute(UuidAuthFilter.CLIENT_ID_ATTR);
    }
}