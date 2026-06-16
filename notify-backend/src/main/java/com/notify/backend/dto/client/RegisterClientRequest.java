package com.notify.backend.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterClientRequest {

    @NotBlank(message = "Client name is required")
    @Size(min = 2, max = 255, message = "Client name must be between 2 and 255 characters")
    private String name;
}