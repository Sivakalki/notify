package com.notify.backend.repository;

import com.notify.backend.entity.ChannelType;
import com.notify.backend.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    List<NotificationTemplate> findByClientId(UUID clientId);

    List<NotificationTemplate> findByClientIdAndChannel(UUID clientId, ChannelType channel);

    Optional<NotificationTemplate> findByClientIdAndName(UUID clientId, String name);
}