package com.notify.backend.repository;

import com.notify.backend.entity.RetryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RetryEventRepository extends JpaRepository<RetryEvent, Long> {

    Optional<RetryEvent> findByEventId(Long eventId);

    // Finds events due for retry and still within max retry limit
    List<RetryEvent> findByNextRetryAtBeforeAndRetryCountLessThan(Instant now, int maxRetries);
}