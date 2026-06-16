package com.notify.backend.dto.common;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;

    @Builder.Default
    private Instant timestamp = Instant.now();
}