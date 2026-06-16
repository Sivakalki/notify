package com.notify.backend.service;

import com.notify.backend.dto.notification.SendNotificationRequest;
import com.notify.backend.dto.notification.SendNotificationResponse;
import com.notify.backend.entity.*;
import com.notify.backend.exception.CampaignNotFoundException;
import com.notify.backend.kafka.producer.NotificationProducer;
import com.notify.backend.repository.*;
import com.notify.backend.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock NotificationCampaignRepository campaignRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationEventRepository eventRepository;
    @Mock NotificationDeliveryRepository deliveryRepository;
    @Mock NotificationProducer producer;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks NotificationServiceImpl service;

    private final UUID clientId = UUID.randomUUID();
    private NotificationCampaign campaign;
    private SendNotificationRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "idempotencyTtlHours", 24L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        campaign = NotificationCampaign.builder()
                .id(10L)
                .channel(ChannelType.EMAIL)
                .message("Hello!")
                .build();

        request = new SendNotificationRequest();
        request.setCampaignId(10L);
        request.setExternalUserId("ext-u1");
        request.setEmail("user@example.com");
    }

    @Test
    void send_newRequest_createsEventAndPublishesToKafka() {
        User user = User.builder().id(1L).externalUserId("ext-u1").email("user@example.com").build();
        NotificationEvent event = NotificationEvent.builder().id(99L).build();

        when(campaignRepository.findByIdAndClientId(10L, clientId)).thenReturn(Optional.of(campaign));
        when(valueOps.get(anyString())).thenReturn(null);
        when(userRepository.findByExternalUserId("ext-u1")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenReturn(user);
        when(eventRepository.save(any())).thenReturn(event);

        SendNotificationResponse response = service.send(request, clientId);

        assertThat(response.isDuplicate()).isFalse();
        assertThat(response.getEventId()).isEqualTo(99L);
        assertThat(response.getStatus()).isEqualTo(DeliveryStatus.PENDING);

        verify(producer).sendToChannel(any());
        verify(valueOps).set(anyString(), eq("99"), any());
        verify(campaignRepository).addTotalUsers(10L, 1);
    }

    @Test
    void send_existingUser_reusesUserRecord() {
        User existing = User.builder().id(5L).externalUserId("ext-u1").build();
        NotificationEvent event = NotificationEvent.builder().id(55L).build();

        when(campaignRepository.findByIdAndClientId(10L, clientId)).thenReturn(Optional.of(campaign));
        when(valueOps.get(anyString())).thenReturn(null);
        when(userRepository.findByExternalUserId("ext-u1")).thenReturn(Optional.of(existing));
        when(eventRepository.save(any())).thenReturn(event);

        service.send(request, clientId);

        verify(userRepository, never()).save(any());
    }

    @Test
    void send_duplicateRequest_returnsDuplicateTrueWithoutPublishing() {
        when(campaignRepository.findByIdAndClientId(10L, clientId)).thenReturn(Optional.of(campaign));
        when(valueOps.get(anyString())).thenReturn("42");

        SendNotificationResponse response = service.send(request, clientId);

        assertThat(response.isDuplicate()).isTrue();
        assertThat(response.getEventId()).isEqualTo(42L);

        verify(producer, never()).sendToChannel(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void send_campaignNotFound_throwsCampaignNotFoundException() {
        when(campaignRepository.findByIdAndClientId(anyLong(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.send(request, clientId))
                .isInstanceOf(CampaignNotFoundException.class)
                .hasMessageContaining("10");
    }
}