package com.notify.backend.repository;

import com.notify.backend.entity.DlqEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DlqEventRepository extends JpaRepository<DlqEvent, Long> {

    // Only returns events that have not been replayed yet
    Page<DlqEvent> findByReplayedAtIsNull(Pageable pageable);

    Optional<DlqEvent> findByEventId(Long eventId);
}