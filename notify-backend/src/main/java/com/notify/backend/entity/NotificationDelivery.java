package com.notify.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification_delivery")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One-to-one: UNIQUE constraint on event_id is enforced in the DB (V6 migration)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private NotificationEvent event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChannelType channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}