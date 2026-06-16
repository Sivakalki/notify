package com.notify.backend.dto.cohort;

import com.notify.backend.entity.Cohort;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortResponse {

    private Long id;
    private String name;
    private String description;
    private int memberCount;
    private Instant createdAt;

    public static CohortResponse from(Cohort cohort) {
        return CohortResponse.builder()
                .id(cohort.getId())
                .name(cohort.getName())
                .description(cohort.getDescription())
                .memberCount(cohort.getMembers().size())
                .createdAt(cohort.getCreatedAt())
                .build();
    }
}
