package com.notify.backend.repository;

import com.notify.backend.entity.DeliveryStatus;
import com.notify.backend.entity.NotificationEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {

    Page<NotificationEvent> findByCampaignId(Long campaignId, Pageable pageable);

    Page<NotificationEvent> findByCampaignIdAndStatus(Long campaignId, DeliveryStatus status, Pageable pageable);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<NotificationEvent> findByIdempotencyKey(String idempotencyKey);

    long countByCampaignIdAndStatus(Long campaignId, DeliveryStatus status);

    long countByStatus(DeliveryStatus status);

    // Single DB round-trip: returns [status, count] pairs for all statuses present
    @Query("SELECT e.status, COUNT(e) FROM NotificationEvent e GROUP BY e.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT e.status, COUNT(e) FROM NotificationEvent e " +
           "WHERE e.createdAt >= :startDate AND e.createdAt <= :endDate " +
           "GROUP BY e.status")
    List<Object[]> countGroupedByStatusInDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT e.status, COUNT(e) FROM NotificationEvent e " +
           "WHERE e.campaign.id IN :campaignIds " +
           "AND e.createdAt >= :startDate AND e.createdAt <= :endDate " +
           "GROUP BY e.status")
    List<Object[]> countGroupedByStatusByCampaigns(
            @Param("campaignIds") List<Long> campaignIds,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE NotificationEvent e SET e.status = :status WHERE e.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") DeliveryStatus status);
}
