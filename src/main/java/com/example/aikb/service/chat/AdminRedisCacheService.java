package com.example.aikb.service.chat;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRedisCacheService {

    private static final long WARN_INTERVAL_MILLIS = 60_000L;

    private final RedisTemplate<String, String> adminJsonRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicLong lastWarnAt = new AtomicLong(0L);

    public <T> Optional<T> get(String key, JavaType valueType, String cacheName) {
        String cacheKey = Objects.requireNonNull(key, "key must not be null");
        try {
            String json = adminJsonRedisTemplate.opsForValue().get(cacheKey);
            if (json == null) {
                log.debug("Admin Redis cache miss, cache={}, key={}", cacheName, cacheKey);
                return Optional.empty();
            }
            log.debug("Admin Redis cache hit, cache={}, key={}", cacheName, cacheKey);
            return Optional.of(objectMapper.readValue(json, valueType));
        } catch (RuntimeException ex) {
            warnRedisUnavailable("read", cacheName, ex);
        } catch (Exception ex) {
            warnRedisUnavailable("deserialize", cacheName, ex);
        }
        return Optional.empty();
    }

    public void put(String key, Object value, Duration ttl, String cacheName) {
        if (value == null || ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        String cacheKey = Objects.requireNonNull(key, "key must not be null");
        Object cacheValue = Objects.requireNonNull(value, "value must not be null");
        Duration cacheTtl = Objects.requireNonNull(ttl, "ttl must not be null");
        try {
            String json = Objects.requireNonNull(objectMapper.writeValueAsString(cacheValue),
                    "serialized cache value must not be null");
            adminJsonRedisTemplate.opsForValue().set(cacheKey, json, cacheTtl);
        } catch (RuntimeException ex) {
            warnRedisUnavailable("write", cacheName, ex);
        } catch (Exception ex) {
            warnRedisUnavailable("serialize", cacheName, ex);
        }
    }

    private void warnRedisUnavailable(String operation, String cacheName, Exception ex) {
        long now = System.currentTimeMillis();
        long previous = lastWarnAt.get();
        if (now - previous >= WARN_INTERVAL_MILLIS && lastWarnAt.compareAndSet(previous, now)) {
            String fallback = "write".equals(operation) || "serialize".equals(operation)
                    ? "ignoring cache write failure"
                    : "falling back to MySQL";
            log.warn("Redis admin cache {} failed, cache={}, {}: {}", operation, cacheName, fallback,
                    ex.getMessage());
        }
    }
}
