package com.example.aikb.service.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.aikb.config.AppAdminCacheProperties;
import com.example.aikb.infra.cache.AdminCacheKeys;
import com.example.aikb.vo.chat.AdminChatStatsVO;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AdminChatStatsCacheServiceTest {

    @Mock
    private RedisTemplate<String, AdminChatStatsVO> redisTemplate;

    @Mock
    private ValueOperations<String, AdminChatStatsVO> valueOperations;

    private AdminChatStatsCacheService cacheService;

    @BeforeEach
    void setUp() {
        AppAdminCacheProperties properties = new AppAdminCacheProperties();
        properties.setStatsTtlMinutes(5L);
        cacheService = new AdminChatStatsCacheService(redisTemplate, properties);
    }

    @Test
    void getBaseStatsReturnsCachedValue() {
        AdminChatStatsVO stats = AdminChatStatsVO.builder().totalChatCount(1L).build();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(AdminCacheKeys.CHAT_STATS)).thenReturn(stats);

        Optional<AdminChatStatsVO> result = cacheService.getBaseStats();

        assertThat(result).containsSame(stats);
    }

    @Test
    void getBaseStatsReturnsEmptyWhenRedisFails() {
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));

        Optional<AdminChatStatsVO> result = cacheService.getBaseStats();

        assertThat(result).isEmpty();
    }

    @Test
    void putBaseStatsWritesWithConfiguredTtl() {
        AdminChatStatsVO stats = AdminChatStatsVO.builder().totalChatCount(1L).build();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.putBaseStats(stats);

        verify(valueOperations).set(AdminCacheKeys.CHAT_STATS, stats, Duration.ofMinutes(5));
    }

    @Test
    void putBaseStatsSwallowsRedisFailures() {
        AdminChatStatsVO stats = AdminChatStatsVO.builder().totalChatCount(1L).build();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RedisConnectionFailureException("down"))
                .when(valueOperations).set(AdminCacheKeys.CHAT_STATS, stats, Duration.ofMinutes(5));

        cacheService.putBaseStats(stats);
    }
}
