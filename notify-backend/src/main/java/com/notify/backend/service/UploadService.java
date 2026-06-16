package com.notify.backend.service;

import com.notify.backend.dto.upload.BulkUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UploadService {
    BulkUploadResponse bulkUpload(MultipartFile file, Long campaignId, UUID clientId);
}
