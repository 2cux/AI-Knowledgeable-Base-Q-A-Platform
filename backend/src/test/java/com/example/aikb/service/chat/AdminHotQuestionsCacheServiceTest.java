package com.example.aikb.service.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.aikb.config.AppAdminCacheProperties;
import com.example.aikb.infra.cache.AdminCacheKeys;
import com.example.aikb.vo.chat.AdminHotQuestionVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminHotQuestionsCacheServiceTest {

    @Mock
    private AdminRedisCacheService adminRedisCacheService;

    private AdminHotQuestionsCacheService cacheService;

    @BeforeEach
    void setUp() {
        AppAdminCacheProperties properties = new AppAdminCacheProperties();
        properties.setHotQuestionsTtlMinutes(5L);
        cacheService = new AdminHotQuestionsCacheService(adminRedisCacheService, properties, new ObjectMapper());
    }

    @Test
    void getHotQuestionsUsesLimitSpecificKey() {
        when(adminRedisCacheService.get(eq(AdminCacheKeys.hotQuestions(10)), any(), eq("hotQuestions")))
                .thenReturn(Optional.of(List.of()));

        cacheService.getHotQuestions(10);

        verify(adminRedisCacheService).get(eq("aikb:admin:chat:hot_questions:10"), any(), eq("hotQuestions"));
    }

    @Test
    void putHotQuestionsWritesWithLimitSpecificKeyAndConfiguredTtl() {
        List<AdminHotQuestionVO> hotQuestions = List.of(AdminHotQuestionVO.builder()
                .question("question")
                .count(3L)
                .latestAskedAt(LocalDateTime.of(2026, 5, 2, 12, 0))
                .build());

        cacheService.putHotQuestions(20, hotQuestions);

        verify(adminRedisCacheService).put("aikb:admin:chat:hot_questions:20", hotQuestions,
                Duration.ofMinutes(5), "hotQuestions");
    }
}
