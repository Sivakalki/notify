package com.notify.backend.service;

import com.notify.backend.service.impl.DeduplicationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeduplicationServiceImplTest {

    @Mock StringRedisTemplate redisTemplate;
    @InjectMocks DeduplicationServiceImpl service;

    // ── Campaign-scoped ───────────────────────────────────────────────────────

    @Test
    void addIfAbsent_newItem_returnsTrue() {
        doReturn(1L).when(redisTemplate).execute(any(RedisCallback.class));

        boolean result = service.addIfAbsent(1L, "user-1");

        assertThat(result).isTrue();
    }

    @Test
    void addIfAbsent_duplicate_returnsFalse() {
        doReturn(0L).when(redisTemplate).execute(any(RedisCallback.class));

        boolean result = service.addIfAbsent(1L, "user-1");

        assertThat(result).isFalse();
    }

    @Test
    void addIfAbsent_nullResult_returnsFalse() {
        doReturn(null).when(redisTemplate).execute(any(RedisCallback.class));

        assertThat(service.addIfAbsent(1L, "user-1")).isFalse();
    }

    @Test
    void exists_presentItem_returnsTrue() {
        doReturn(1L).when(redisTemplate).execute(any(RedisCallback.class));

        assertThat(service.exists(1L, "user-1")).isTrue();
    }

    @Test
    void exists_absentItem_returnsFalse() {
        doReturn(0L).when(redisTemplate).execute(any(RedisCallback.class));

        assertThat(service.exists(1L, "user-1")).isFalse();
    }

    @Test
    void delete_invokesRedisExecute() {
        doReturn(null).when(redisTemplate).execute(any(RedisCallback.class));

        service.delete(1L, "user-1");

        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    // ── Cohort-scoped ─────────────────────────────────────────────────────────

    @Test
    void addCohortMemberIfAbsent_newMember_returnsTrue() {
        doReturn(1L).when(redisTemplate).execute(any(RedisCallback.class));

        assertThat(service.addCohortMemberIfAbsent(5L, "user-x")).isTrue();
    }

    @Test
    void addCohortMemberIfAbsent_duplicate_returnsFalse() {
        doReturn(0L).when(redisTemplate).execute(any(RedisCallback.class));

        assertThat(service.addCohortMemberIfAbsent(5L, "user-x")).isFalse();
    }

    @Test
    void removeCohortMember_invokesRedisExecute() {
        doReturn(null).when(redisTemplate).execute(any(RedisCallback.class));

        service.removeCohortMember(5L, "user-x");

        verify(redisTemplate).execute(any(RedisCallback.class));
    }
}