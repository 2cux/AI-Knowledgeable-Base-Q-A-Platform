package com.example.aikb.service.chat;

import com.example.aikb.config.AppAdminCacheProperties;
import com.example.aikb.infra.cache.AdminCacheKeys;
import com.example.aikb.vo.chat.AdminHotQuestionVO;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminHotQuestionsCacheService {

    private static final String CACHE_NAME = "hotQuestions";

    private final AdminRedisCacheService adminRedisCacheService;
    private final AppAdminCacheProperties adminCacheProperties;
    private final ObjectMapper objectMapper;

    public Optional<List<AdminHotQuestionVO>> getHotQuestions(int limit) {
        JavaType valueType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, AdminHotQuestionVO.class);
        return adminRedisCacheService.get(AdminCacheKeys.hotQuestions(limit), valueType, CACHE_NAME);
    }

    public void putHotQuestions(int limit, List<AdminHotQuestionVO> hotQuestions) {
        adminRedisCacheService.put(AdminCacheKeys.hotQuestions(limit), hotQuestions,
                Duration.ofMinutes(adminCacheProperties.getHotQuestionsTtlMinutes()), CACHE_NAME);
    }
}
