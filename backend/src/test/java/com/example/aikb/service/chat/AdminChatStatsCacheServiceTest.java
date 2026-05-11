package com.example.aikb.service.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.aikb.config.AppAdminCacheProperties;
import com.example.aikb.infra.cache.AdminCacheKeys;
import com.example.aikb.vo.chat.AdminChatStatsVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminChatStatsCacheServiceTest {

    @Mock
    private AdminRedisCacheService adminRedisCacheService;

    private AdminChatStatsCacheService cacheService;

    @BeforeEach
    void setUp() {
        AppAdminCacheProperties properties = new AppAdminCacheProperties();
        properties.setStatsTtlMinutes(5L);
        cacheService = new AdminChatStatsCacheService(adminRedisCacheService, properties, new ObjectMapper());
    }

    @Test
    void getBaseStatsReadsFixedKey() {
        AdminChatStatsVO stats = AdminChatStatsVO.builder().totalChatCount(1L).build();
        when(adminRedisCacheService.get(eq(AdminCacheKeys.CHAT_STATS), any(), eq("chatStats")))
                .thenReturn(Optional.of(stats));

        cacheService.getBaseStats();

        verify(adminRedisCacheService).get(eq(AdminCacheKeys.CHAT_STATS), any(), eq("chatStats"));
    }

    @Test
    void putBaseStatsWritesWithConfiguredTtl() {
        AdminChatStatsVO stats = AdminChatStatsVO.builder().totalChatCount(1L).build();

        cacheService.putBaseStats(stats);

        verify(adminRedisCacheService).put(AdminCacheKeys.CHAT_STATS, stats, Duration.ofMinutes(5), "chatStats");
    }
}
