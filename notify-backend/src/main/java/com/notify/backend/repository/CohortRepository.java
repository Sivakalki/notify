package com.notify.backend.repository;

import com.notify.backend.entity.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CohortRepository extends JpaRepository<Cohort, Long> {

    List<Cohort> findByClientId(UUID clientId);

    Optional<Cohort> findByIdAndClientId(Long id, UUID clientId);

    boolean existsByClientIdAndName(UUID clientId, String name);
}