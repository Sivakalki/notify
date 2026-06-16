package com.notify.backend.filter;

import tools.jackson.databind.json.JsonMapper;
import com.notify.backend.dto.common.ErrorResponse;
import com.notify.backend.entity.FrontendClient;
import com.notify.backend.repository.FrontendClientRepository;
import com.notify.backend.util.HashUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UuidAuthFilter extends OncePerRequestFilter {

    public static final String CLIENT_ID_ATTR = "clientId";
    private static final String AUTH_HEADER   = "X-Client-UUID";

    private final FrontendClientRepository clientRepository;
    private final StringRedisTemplate redisTemplate;
    private final JsonMapper objectMapper;

    @Value("${notify.security.rate-limit-per-minute:1000}")
    private long rateLimitPerMinute;

    @Value("${notify.security.auth-cache-ttl-hours:1}")
    private long authCacheTtlHours;

    // Paths and methods that skip auth entirely
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/clients")
                || path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String rawApiKey = request.getHeader(AUTH_HEADER);
        if (rawApiKey == null || rawApiKey.isBlank()) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Missing " + AUTH_HEADER + " header");
            return;
        }

        String apiKeyHash = HashUtil.sha256(rawApiKey.trim());
        UUID clientId = resolveClientId(apiKeyHash);

        if (clientId == null) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Invalid or inactive API key");
            return;
        }

        if (isRateLimitExceeded(clientId)) {
            log.warn("Rate limit exceeded for clientId={}", clientId);
            writeError(response, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Try again in a minute.");
            return;
        }

        // Downstream services read clientId from here — no need to re-query Redis
        request.setAttribute(CLIENT_ID_ATTR, clientId);
        chain.doFilter(request, response);
    }

    // Checks Redis first; falls back to DB and re-warms the cache on a miss
    private UUID resolveClientId(String apiKeyHash) {
        String cacheKey = "auth:client:" + apiKeyHash;
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            return UUID.fromString(cached);
        }

        return clientRepository.findByApiKeyHash(apiKeyHash)
                .filter(FrontendClient::isActive)
                .map(client -> {
                    redisTemplate.opsForValue().set(cacheKey, client.getId().toString(),
                            Duration.ofHours(authCacheTtlHours));
                    return client.getId();
                })
                .orElse(null);
    }

    // Sliding window per minute using Redis INCR + EXPIRE
    private boolean isRateLimitExceeded(UUID clientId) {
        long minuteBucket = System.currentTimeMillis() / 60_000;
        String rateKey = "rate:" + clientId + ":" + minuteBucket;

        Long count = redisTemplate.opsForValue().increment(rateKey);
        if (count != null && count == 1) {
            // Set TTL only on first increment so the key auto-expires after 60s
            redisTemplate.expire(rateKey, Duration.ofSeconds(60));
        }
        return count != null && count > rateLimitPerMinute;
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(Instant.now())
                .build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}