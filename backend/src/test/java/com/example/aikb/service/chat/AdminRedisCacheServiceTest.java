package com.example.aikb.service.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.aikb.vo.chat.AdminHotQuestionVO;
import com.fasterxml.jackson.databind.JavaType;
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
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AdminRedisCacheServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private AdminRedisCacheService cacheService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        cacheService = new AdminRedisCacheService(stringRedisTemplate, objectMapper);
    }

    @Test
    void getDeserializesHotQuestionListWithLocalDateTime() throws Exception {
        List<AdminHotQuestionVO> hotQuestions = List.of(AdminHotQuestionVO.builder()
                .question("question")
                .count(3L)
                .latestAskedAt(LocalDateTime.of(2026, 5, 2, 12, 0))
                .build());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key")).thenReturn(objectMapper.writeValueAsString(hotQuestions));
        JavaType valueType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, AdminHotQuestionVO.class);

        Optional<List<AdminHotQuestionVO>> result = cacheService.get("key", valueType, "hotQuestions");

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
        assertThat(result.get().get(0).getLatestAskedAt()).isEqualTo(LocalDateTime.of(2026, 5, 2, 12, 0));
    }

    @Test
    void getReturnsEmptyWhenRedisFails() {
        when(stringRedisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));

        Optional<String> result = cacheService.get("key", objectMapper.constructType(String.class), "cache");

        assertThat(result).isEmpty();
    }

    @Test
    void putWritesJsonWithTtl() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        AdminHotQuestionVO hotQuestion = AdminHotQuestionVO.builder().question("question").count(1L).build();

        cacheService.put("key", hotQuestion, Duration.ofMinutes(5), "hotQuestions");

        verify(valueOperations).set("key", objectMapper.writeValueAsString(hotQuestion), Duration.ofMinutes(5));
    }

    @Test
    void putSwallowsRedisFailures() {
        AdminHotQuestionVO hotQuestion = AdminHotQuestionVO.builder().question("question").count(1L).build();
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RedisConnectionFailureException("down"))
                .when(valueOperations).set("key", "{\"question\":\"question\",\"count\":1,\"latestAskedAt\":null}",
                        Duration.ofMinutes(5));

        cacheService.put("key", hotQuestion, Duration.ofMinutes(5), "hotQuestions");
    }
}
