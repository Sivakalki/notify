package com.notify.backend.repository;

import com.notify.backend.TestcontainersConfiguration;
import com.notify.backend.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@Transactional
@Rollback
class NotificationCampaignRepositoryIT {

    @Autowired NotificationCampaignRepository campaignRepository;
    @Autowired FrontendClientRepository clientRepository;

    @Test
    void save_andFindByClientId_returnsSavedCampaign() {
        FrontendClient client = savedClient();
        NotificationCampaign campaign = campaignRepository.save(campaign(client, "Test Campaign"));

        Page<NotificationCampaign> page = campaignRepository.findByClientId(
                client.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCampaignName()).isEqualTo("Test Campaign");
        assertThat(page.getContent().get(0).getId()).isEqualTo(campaign.getId());
    }

    @Test
    void findByIdAndClientId_wrongClient_returnsEmpty() {
        FrontendClient client = savedClient();
        NotificationCampaign campaign = campaignRepository.save(campaign(client, "My Campaign"));

        var result = campaignRepository.findByIdAndClientId(campaign.getId(), UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void addTotalUsers_increasesTotalUsersByCount() {
        FrontendClient client = savedClient();
        NotificationCampaign campaign = campaignRepository.save(campaign(client, "Bulk Test"));

        campaignRepository.addTotalUsers(campaign.getId(), 50);

        NotificationCampaign updated = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertThat(updated.getTotalUsers()).isEqualTo(50);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FrontendClient savedClient() {
        return clientRepository.save(FrontendClient.builder()
                .name("Test Client")
                .apiKeyHash(UUID.randomUUID().toString())
                .build());
    }

    private NotificationCampaign campaign(FrontendClient client, String name) {
        return NotificationCampaign.builder()
                .client(client)
                .campaignName(name)
                .message("Hello!")
                .channel(ChannelType.EMAIL)
                .build();
    }
}
