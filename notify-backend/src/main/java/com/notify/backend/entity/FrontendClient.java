package com.notify.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "frontend_clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FrontendClient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    // SHA-256(rawUUID + hostIp) — the raw key is never stored
    @Column(name = "api_key_hash", nullable = false, unique = true)
    private String apiKeyHash;

    @Column(name = "host_ip", length = 45)
    private String hostIp;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
