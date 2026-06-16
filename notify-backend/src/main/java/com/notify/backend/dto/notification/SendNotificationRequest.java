package com.notify.backend.dto.notification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendNotificationRequest {

    @NotNull(message = "Campaign ID is required")
    private Long campaignId;

    @NotBlank(message = "External user ID is required")
    private String externalUserId;

    @Email(message = "Must be a valid email address")
    private String email;

    private String phone;
}