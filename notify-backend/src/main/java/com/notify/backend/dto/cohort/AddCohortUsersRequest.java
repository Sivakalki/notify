package com.notify.backend.dto.cohort;

import com.notify.backend.dto.upload.UserRow;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddCohortUsersRequest {

    @NotEmpty(message = "At least one user is required")
    private List<UserRow> users;
}
