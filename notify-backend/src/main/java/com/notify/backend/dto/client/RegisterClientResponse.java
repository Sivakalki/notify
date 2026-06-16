package com.notify.backend.dto.client;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class RegisterClientResponse {

    private UUID clientId;

    // Raw UUID shown exactly once — never stored, never returned again
    private String apiKey;

    private String message;
}