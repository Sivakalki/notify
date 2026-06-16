package com.notify.backend.dto.cohort;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCohortUsersResponse {

    private int total;
    private int added;
    private int duplicates;
}
