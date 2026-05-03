package com.example.aikb.service.chat;

import com.example.aikb.config.AppAdminCacheProperties;
import com.example.aikb.infra.cache.AdminCacheKeys;
import com.example.aikb.vo.chat.AdminChatStatsVO;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminChatStatsCacheService {

    private static final long WARN_INTERVAL_MILLIS = 60_000L;

    private final RedisTemplate<String, AdminChatStatsVO> adminChatStatsRedisTemplate;
    private final AppAdminCacheProperties adminCacheProperties;
    private final AtomicLong lastWarnAt = new AtomicLong(0L);

    public Optional<AdminChatStatsVO> getBaseStats() {
        try {
            AdminChatStatsVO stats = adminChatStatsRedisTemplate.opsForValue().get(AdminCacheKeys.CHAT_STATS);
            if (stats != null) {
                log.debug("Admin chat stats cache hit, key={}", AdminCacheKeys.CHAT_STATS);
                return Optional.of(stats);
            }
            log.debug("Admin chat stats cache miss, key={}", AdminCacheKeys.CHAT_STATS);
        } catch (RuntimeException ex) {
            warnRedisUnavailable("read", ex);
        }
        return Optional.empty();
    }

    public void putBaseStats(AdminChatStatsVO stats) {
        if (stats == null) {
            return;
        }
        try {
            Duration ttl = Duration.ofMinutes(adminCacheProperties.getStatsTtlMinutes());
            if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                return;
            }
            adminChatStatsRedisTemplate.opsForValue().set(AdminCacheKeys.CHAT_STATS, stats, ttl);
        } catch (RuntimeException ex) {
            warnRedisUnavailable("write", ex);
        }
    }

    private void warnRedisUnavailable(String operation, RuntimeException ex) {
        long now = System.currentTimeMillis();
        long previous = lastWarnAt.get();
        if (now - previous >= WARN_INTERVAL_MILLIS && lastWarnAt.compareAndSet(previous, now)) {
            log.warn("Redis admin stats cache {} failed, falling back to MySQL: {}", operation, ex.getMessage());
        }
    }
}
