package com.notify.backend.filter;

import com.notify.backend.TestcontainersConfiguration;
import com.notify.backend.entity.FrontendClient;
import com.notify.backend.repository.FrontendClientRepository;
import com.notify.backend.util.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "spring.kafka.listener.auto-startup=false")
class UuidAuthFilterIT {

    @Autowired TestRestTemplate restTemplate;
    @Autowired FrontendClientRepository clientRepository;

    private static final String RAW_API_KEY = "test-api-key-" + UUID.randomUUID();
    private String registeredClientId;

    @BeforeEach
    void registerClient() {
        // Only save if not already present (BeforeEach runs per test but shares DB)
        String hash = HashUtil.sha256(RAW_API_KEY);
        FrontendClient existing = clientRepository.findByApiKeyHash(hash).orElse(null);
        if (existing == null) {
            FrontendClient client = clientRepository.save(FrontendClient.builder()
                    .name("Filter Test Client")
                    .apiKeyHash(hash)
                    .build());
            registeredClientId = client.getId().toString();
        } else {
            registeredClientId = existing.getId().toString();
        }
    }

    @Test
    void request_withoutApiKeyHeader_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/campaigns", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void request_withInvalidApiKey_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Client-UUID", "totally-invalid-key");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/campaigns", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void request_withValidApiKey_returnsOk() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Client-UUID", RAW_API_KEY);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/campaigns", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        // 200 or any non-401 means the filter passed the request through
        assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void request_toExcludedPath_passesWithoutApiKey() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}