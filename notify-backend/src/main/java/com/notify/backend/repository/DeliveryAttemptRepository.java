package com.notify.backend.repository;

import com.notify.backend.entity.DeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, Long> {

    List<DeliveryAttempt> findByEventIdOrderByAttemptNumberAsc(Long eventId);

    int countByEventId(Long eventId);
}
