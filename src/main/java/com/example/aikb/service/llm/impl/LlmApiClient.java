package com.example.aikb.service.llm.impl;

import com.example.aikb.common.LogSanitizer;
import com.example.aikb.config.AppLlmProperties;
import com.example.aikb.dto.llm.request.LlmContentItem;
import com.example.aikb.dto.llm.request.LlmMessageRequest;
import com.example.aikb.dto.llm.request.LlmRequest;
import com.example.aikb.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Base HTTP client for the third-party LLM API.
 */
@Slf4j
@Component
public class LlmApiClient {

    private final AppLlmProperties properties;
    private final RestTemplate restTemplate;

    public LlmApiClient(
            AppLlmProperties properties,
            @Qualifier("llmRestTemplate") RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public JsonNode chat(LlmRequest request) {
        String baseUrl = requireText(properties.getBaseUrl(), "LLM base-url 未配置");
        String apiKey = requireText(properties.getApiKey(),
                "LLM api-key 未配置，请通过环境变量 APP_LLM_API_KEY 或 OPENAI_API_KEY 注入");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LlmRequest> entity = new HttpEntity<>(request, headers);
        long start = System.currentTimeMillis();
        try {
            log.info("LLM API request. model={}, messageCount={}, promptLength={}, questionPreview={}",
                    request.getModel(),
                    request.getMessages() == null ? 0 : request.getMessages().size(),
                    resolvePromptLength(request),
                    resolveQuestionPreview(request));
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl, entity, JsonNode.class);
            JsonNode body = response.getBody();
            log.info("LLM API response. model={}, statusCode={}, durationMs={}, outputLength={}",
                    request.getModel(),
                    response.getStatusCode().value(),
                    System.currentTimeMillis() - start,
                    body == null ? 0 : body.toString().length());
            return body;
        } catch (RestClientException ex) {
            log.warn("LLM API call failed. model={}, durationMs={}, errorType={}, error={}",
                    request.getModel(),
                    System.currentTimeMillis() - start,
                    ex.getClass().getSimpleName(),
                    LogSanitizer.safeMessage(ex.getMessage()));
            throw new BusinessException(50000, "LLM API 调用失败");
        }
    }

    @NonNull
    private String requireText(@Nullable String value, String message) {
        if (value == null) {
            throw new BusinessException(50000, message);
        }
        String trimmedValue = value.trim();
        if (trimmedValue.isBlank()) {
            throw new BusinessException(50000, message);
        }
        return trimmedValue;
    }

    private int resolvePromptLength(LlmRequest request) {
        if (request == null || request.getMessages() == null) {
            return 0;
        }
        return request.getMessages().stream()
                .mapToInt(message -> messageTextLength(message.getContent()))
                .sum();
    }

    private String resolveQuestionPreview(LlmRequest request) {
        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()) {
            return "<none>";
        }
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            LlmMessageRequest message = request.getMessages().get(i);
            if (message != null && "user".equalsIgnoreCase(message.getRole())) {
                return LogSanitizer.preview(firstText(message.getContent()), 80);
            }
        }
        return "<none>";
    }

    private int messageTextLength(List<LlmContentItem> content) {
        if (content == null) {
            return 0;
        }
        return content.stream()
                .map(LlmContentItem::getText)
                .filter(text -> text != null)
                .mapToInt(String::length)
                .sum();
    }

    private String firstText(List<LlmContentItem> content) {
        if (content == null) {
            return null;
        }
        return content.stream()
                .map(LlmContentItem::getText)
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElse(null);
    }
}
