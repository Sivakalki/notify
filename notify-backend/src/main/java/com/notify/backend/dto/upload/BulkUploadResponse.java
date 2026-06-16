package com.notify.backend.dto.upload;

import com.notify.backend.entity.FileStatus;
import com.notify.backend.entity.FileType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResponse {
    private Long uploadId;
    private Long campaignId;
    private String fileName;
    private FileType fileType;
    private int rowCount;
    private int duplicateCount;
    private FileStatus status;
    private Instant createdAt;
}
