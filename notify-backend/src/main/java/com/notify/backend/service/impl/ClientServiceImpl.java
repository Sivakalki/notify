package com.notify.backend.service.impl;

import com.notify.backend.dto.client.RegisterClientRequest;
import com.notify.backend.dto.client.RegisterClientResponse;
import com.notify.backend.entity.FrontendClient;
import com.notify.backend.repository.FrontendClientRepository;
import com.notify.backend.service.ClientService;
import com.notify.backend.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final FrontendClientRepository clientRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${notify.security.auth-cache-ttl-hours:1}")
    private long authCacheTtlHours;

    @Override
    @Transactional
    public RegisterClientResponse register(RegisterClientRequest request, String remoteIp) {
        UUID rawApiKey = UUID.randomUUID();
        String apiKeyHash = HashUtil.sha256(rawApiKey.toString());

        FrontendClient client = FrontendClient.builder()
                .name(request.getName())
                .apiKeyHash(apiKeyHash)
                .hostIp(remoteIp)
                .build();

        FrontendClient saved = clientRepository.save(client);

        // Pre-warm the auth cache so the first request doesn't hit the DB
        String cacheKey = "auth:client:" + apiKeyHash;
        redisTemplate.opsForValue().set(cacheKey, saved.getId().toString(),
                Duration.ofHours(authCacheTtlHours));

        log.info("Registered new client '{}' with id={} from ip={}", saved.getName(), saved.getId(), remoteIp);

        return RegisterClientResponse.builder()
                .clientId(saved.getId())
                .apiKey(rawApiKey.toString())
                .message("Registration successful. Store this API key securely — it will not be shown again.")
                .build();
    }
}