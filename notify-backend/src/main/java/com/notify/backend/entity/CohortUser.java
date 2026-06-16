package com.notify.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "cohort_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CohortUser {

    @EmbeddedId
    private CohortUserId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("cohortId")
    @JoinColumn(name = "cohort_id")
    private Cohort cohort;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;
}