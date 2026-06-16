package com.notify.backend.service.impl;

import com.notify.backend.dto.upload.BulkUploadResponse;
import com.notify.backend.entity.FileStatus;
import com.notify.backend.entity.FileType;
import com.notify.backend.entity.NotificationCampaign;
import com.notify.backend.entity.UploadedFile;
import com.notify.backend.exception.CampaignNotFoundException;
import com.notify.backend.repository.NotificationCampaignRepository;
import com.notify.backend.repository.UploadedFileRepository;
import com.notify.backend.service.UploadService;
import com.notify.backend.upload.BulkUploadProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final NotificationCampaignRepository campaignRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final BulkUploadProcessor processor;

    @Override
    @Transactional
    public BulkUploadResponse bulkUpload(MultipartFile file, Long campaignId, UUID clientId) {
        NotificationCampaign campaign = campaignRepository.findByIdAndClientId(campaignId, clientId)
                .orElseThrow(() -> new CampaignNotFoundException("Campaign not found: " + campaignId));

        FileType fileType = detectFileType(file.getOriginalFilename());

        UploadedFile upload = uploadedFileRepository.save(UploadedFile.builder()
                .campaign(campaign)
                .fileName(file.getOriginalFilename())
                .fileType(fileType)
                .build());

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            upload.setStatus(FileStatus.FAILED);
            uploadedFileRepository.save(upload);
            throw new RuntimeException("Failed to read uploaded file", e);
        }

        // Fire-and-forget — transaction commits before the async task starts
        processor.processAsync(upload.getId(), campaign.getId(), fileBytes, fileType);

        log.info("Bulk upload queued: uploadId={}, campaignId={}, file={}, type={}",
                upload.getId(), campaignId, upload.getFileName(), fileType);

        return BulkUploadResponse.builder()
                .uploadId(upload.getId())
                .campaignId(campaignId)
                .fileName(upload.getFileName())
                .fileType(fileType)
                .status(FileStatus.PROCESSING)
                .createdAt(upload.getCreatedAt())
                .build();
    }

    private FileType detectFileType(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("File name is required to determine type");
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".csv"))                         return FileType.CSV;
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return FileType.EXCEL;
        if (lower.endsWith(".json"))                        return FileType.JSON;
        throw new IllegalArgumentException(
                "Unsupported file type. Accepted extensions: .csv, .xlsx, .xls, .json");
    }
}
