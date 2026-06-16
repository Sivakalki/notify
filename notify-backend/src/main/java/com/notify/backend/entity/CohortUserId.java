package com.notify.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CohortUserId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "cohort_id")
    private Long cohortId;

    @Column(name = "user_id")
    private Long userId;
}