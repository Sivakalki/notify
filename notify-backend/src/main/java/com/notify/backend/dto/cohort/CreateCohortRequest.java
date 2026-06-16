package com.notify.backend.dto.cohort;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCohortRequest {

    @NotBlank(message = "Cohort name is required")
    @Size(max = 255, message = "Cohort name must not exceed 255 characters")
    private String name;

    private String description;
}
