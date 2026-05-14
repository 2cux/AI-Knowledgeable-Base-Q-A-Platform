package com.example.aikb.service.llm.impl;

import com.example.aikb.common.LogSanitizer;
import com.example.aikb.config.AppLlmProperties;
import com.example.aikb.dto.llm.request.LlmMessageRequest;
import com.example.aikb.dto.llm.request.LlmRequest;
import com.example.aikb.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * Base HTTP client for the Claude Messages compatible third-party LLM API.
 */
@Slf4j
@Component
public class LlmApiClient {

    private static final String EXAMPLE_HOST_MARKER = "example.com";
    private static final int ERROR_BODY_PREVIEW_LENGTH = 300;

    private final AppLlmProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LlmApiClient(
            AppLlmProperties properties,
            @Qualifier("llmRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String chat(LlmRequest request) {
        String finalUrl = normalizeFinalUrl(requireText(properties.getBaseUrl(), "LLM base-url 未配置"));
        rejectPlaceholderBaseUrl(finalUrl);
        String apiKey = requireText(properties.getApiKey(),
                "LLM api-key 未配置，请通过环境变量 APP_LLM_API_KEY 注入");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<LlmRequest> entity = new HttpEntity<>(request, headers);
        long start = System.currentTimeMillis();
        try {
            log.info("LLM API request. finalUrl={}, model={}, messageCount={}, promptLength={}, questionPreview={}",
                    finalUrl,
                    request.getModel(),
                    request.getMessages() == null ? 0 : request.getMessages().size(),
                    resolvePromptLength(request),
                    resolveQuestionPreview(request));
            ResponseEntity<String> response = restTemplate.postForEntity(finalUrl, entity, String.class);
            MediaType contentType = response.getHeaders().getContentType();
            String body = response.getBody();
            if (!isJsonContentType(contentType)) {
                log.warn("LLM API returned non-JSON response. finalUrl={}, statusCode={}, contentType={}, bodyPreview={}",
                        finalUrl,
                        response.getStatusCode().value(),
                        contentType,
                        LogSanitizer.sanitize(body, ERROR_BODY_PREVIEW_LENGTH));
                throw new BusinessException(50000, "LLM API 返回非 JSON 响应");
            }
            validateJsonBody(body, finalUrl, response.getStatusCode().value(), contentType);
            log.info("LLM API response. finalUrl={}, model={}, statusCode={}, contentType={}, durationMs={}, outputLength={}",
                    finalUrl,
                    request.getModel(),
                    response.getStatusCode().value(),
                    contentType,
                    System.currentTimeMillis() - start,
                    body == null ? 0 : body.length());
            return body;
        } catch (RestClientResponseException ex) {
            MediaType contentType = ex.getResponseHeaders() == null ? null : ex.getResponseHeaders().getContentType();
            log.warn("LLM API HTTP call failed. finalUrl={}, statusCode={}, contentType={}, bodyPreview={}, durationMs={}",
                    finalUrl,
                    ex.getStatusCode().value(),
                    contentType,
                    LogSanitizer.sanitize(ex.getResponseBodyAsString(), ERROR_BODY_PREVIEW_LENGTH),
                    System.currentTimeMillis() - start);
            throw new BusinessException(50000, "LLM API 调用失败: HTTP " + ex.getStatusCode().value());
        } catch (BusinessException ex) {
            throw ex;
        } catch (org.springframework.web.client.ResourceAccessException ex) {
            String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
            String errorMsg;
            if (message.contains("tim") || message.contains("read timed out") || message.contains("connect timed out")) {
                errorMsg = "LLM API 调用超时，可能是模型响应较慢，请稍后重试";
                log.warn("LLM API call timed out. finalUrl={}, model={}, durationMs={}, error={}",
                        finalUrl, request.getModel(), System.currentTimeMillis() - start,
                        LogSanitizer.safeMessage(ex.getMessage()));
            } else if (message.contains("connect") || message.contains("refused") || message.contains("econn")) {
                errorMsg = "LLM API 连接失败，请检查网络或 API 服务状态";
                log.warn("LLM API connection failed. finalUrl={}, model={}, durationMs={}, error={}",
                        finalUrl, request.getModel(), System.currentTimeMillis() - start,
                        LogSanitizer.safeMessage(ex.getMessage()));
            } else {
                errorMsg = "LLM API 通信异常";
                log.warn("LLM API communication error. finalUrl={}, model={}, durationMs={}, errorType={}, error={}",
                        finalUrl, request.getModel(), System.currentTimeMillis() - start,
                        ex.getClass().getSimpleName(), LogSanitizer.safeMessage(ex.getMessage()));
            }
            throw new BusinessException(50000, errorMsg);
        } catch (RestClientException ex) {
            log.warn("LLM API call failed. finalUrl={}, model={}, durationMs={}, errorType={}, error={}",
                    finalUrl,
                    request.getModel(),
                    System.currentTimeMillis() - start,
                    ex.getClass().getSimpleName(),
                    LogSanitizer.safeMessage(ex.getMessage()));
            throw new BusinessException(50000, "LLM API 调用失败: " + LogSanitizer.safeMessage(ex.getMessage()));
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

    private String normalizeFinalUrl(String baseUrl) {
        return baseUrl.trim().replaceAll("/+$", "");
    }

    private int resolvePromptLength(LlmRequest request) {
        if (request == null || request.getMessages() == null) {
            return 0;
        }
        int messageLength = request.getMessages().stream()
                .map(LlmMessageRequest::getContent)
                .filter(content -> content != null)
                .mapToInt(String::length)
                .sum();
        return messageLength + (request.getSystem() == null ? 0 : request.getSystem().length());
    }

    private void rejectPlaceholderBaseUrl(String baseUrl) {
        if (baseUrl.toLowerCase().contains(EXAMPLE_HOST_MARKER)) {
            throw new BusinessException(50000, "LLM baseUrl 未配置或仍为占位值，请设置 APP_LLM_BASE_URL");
        }
    }

    private String resolveQuestionPreview(LlmRequest request) {
        if (request == null || request.getMessages() == null || request.getMessages().isEmpty()) {
            return "<none>";
        }
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            LlmMessageRequest message = request.getMessages().get(i);
            if (message != null && "user".equalsIgnoreCase(message.getRole())) {
                return LogSanitizer.preview(message.getContent(), 80);
            }
        }
        return "<none>";
    }

    private boolean isJsonContentType(@Nullable MediaType contentType) {
        if (contentType == null) {
            return false;
        }
        return MediaType.APPLICATION_JSON.includes(contentType)
                || contentType.getSubtype().toLowerCase(java.util.Locale.ROOT).endsWith("+json");
    }

    private void validateJsonBody(String body, String finalUrl, int statusCode, @Nullable MediaType contentType) {
        if (!StringUtils.hasText(body)) {
            log.warn("LLM API returned empty JSON body. finalUrl={}, statusCode={}, contentType={}",
                    finalUrl,
                    statusCode,
                    contentType);
            throw new BusinessException(50000, "LLM API 返回空 JSON 响应");
        }
        try {
            objectMapper.readTree(body);
        } catch (JsonProcessingException ex) {
            log.warn("LLM API returned invalid JSON. finalUrl={}, statusCode={}, contentType={}, bodyPreview={}",
                    finalUrl,
                    statusCode,
                    contentType,
                    LogSanitizer.sanitize(body, ERROR_BODY_PREVIEW_LENGTH));
            throw new BusinessException(50000, "LLM API 返回无效 JSON");
        }
    }
}
