package com.notify.backend.repository;

import com.notify.backend.entity.CampaignStatus;
import com.notify.backend.entity.NotificationCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationCampaignRepository extends JpaRepository<NotificationCampaign, Long> {

    Page<NotificationCampaign> findByClientId(UUID clientId, Pageable pageable);

    Page<NotificationCampaign> findByClientIdAndStatus(UUID clientId, CampaignStatus status, Pageable pageable);

    Optional<NotificationCampaign> findByIdAndClientId(Long id, UUID clientId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE NotificationCampaign c SET c.totalUsers = c.totalUsers + :count, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void addTotalUsers(@Param("id") Long id, @Param("count") int count);
}