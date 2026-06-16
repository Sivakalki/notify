package com.notify.backend.repository;

import com.notify.backend.entity.DeliveryStatus;
import com.notify.backend.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    Optional<NotificationDelivery> findByEventId(Long eventId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE NotificationDelivery d SET d.status = :status, d.deliveredAt = :deliveredAt, d.errorMessage = :errorMessage WHERE d.event.id = :eventId")
    void updateByEventId(@Param("eventId") Long eventId,
                         @Param("status") DeliveryStatus status,
                         @Param("deliveredAt") Instant deliveredAt,
                         @Param("errorMessage") String errorMessage);
}
