package com.notify.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "dlq_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DlqEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One DLQ record per event — UNIQUE enforced in DB (V9 migration)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private NotificationEvent event;

    @Column(columnDefinition = "TEXT")
    private String reason;

    // Full Kafka message payload stored as JSONB for replay
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "replayed_at")
    private Instant replayedAt;
}