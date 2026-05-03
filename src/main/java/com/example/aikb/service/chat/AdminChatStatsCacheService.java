package com.example.aikb.service.chat;

import com.example.aikb.config.AppAdminCacheProperties;
import com.example.aikb.infra.cache.AdminCacheKeys;
import com.example.aikb.vo.chat.AdminChatStatsVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminChatStatsCacheService {

    private static final String CACHE_NAME = "chatStats";

    private final AdminRedisCacheService adminRedisCacheService;
    private final AppAdminCacheProperties adminCacheProperties;
    private final ObjectMapper objectMapper;

    public Optional<AdminChatStatsVO> getBaseStats() {
        return adminRedisCacheService.get(AdminCacheKeys.CHAT_STATS,
                objectMapper.constructType(AdminChatStatsVO.class), CACHE_NAME);
    }

    public void putBaseStats(AdminChatStatsVO stats) {
        adminRedisCacheService.put(AdminCacheKeys.CHAT_STATS, stats,
                Duration.ofMinutes(adminCacheProperties.getStatsTtlMinutes()), CACHE_NAME);
    }
}
