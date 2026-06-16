package com.notify.backend.repository;

import com.notify.backend.entity.FrontendClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FrontendClientRepository extends JpaRepository<FrontendClient, UUID> {

    Optional<FrontendClient> findByApiKeyHash(String apiKeyHash);

    boolean existsByApiKeyHash(String apiKeyHash);
}