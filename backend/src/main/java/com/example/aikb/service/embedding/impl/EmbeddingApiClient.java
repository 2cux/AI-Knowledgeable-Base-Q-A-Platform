package com.example.aikb.service.embedding.impl;

import com.example.aikb.common.LogSanitizer;
import com.example.aikb.config.AppEmbeddingProperties;
import com.example.aikb.dto.embedding.request.EmbeddingInputItem;
import com.example.aikb.dto.embedding.request.EmbeddingRequest;
import com.example.aikb.dto.embedding.response.EmbeddingResponse;
import com.example.aikb.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * Base HTTP client for the third-party embedding API.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.embedding", name = "enabled", havingValue = "true")
public class EmbeddingApiClient {

    private static final String EXAMPLE_HOST_MARKER = "example.com";

    private final AppEmbeddingProperties properties;
    private final RestTemplate restTemplate;

    public EmbeddingApiClient(
            AppEmbeddingProperties properties,
            @Qualifier("embeddingRestTemplate") RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public EmbeddingResponse embed(EmbeddingRequest request) {
        String baseUrl = requireText(properties.getBaseUrl(), "embedding base-url 未配置");
        rejectPlaceholderBaseUrl(baseUrl);
        String apiKey = requireText(properties.getApiKey(),
                "embedding api-key 未配置，请通过环境变量 APP_EMBEDDING_API_KEY 或 OPENAI_API_KEY 注入");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(request, headers);
        long start = System.currentTimeMillis();
        try {
            log.info("Embedding API request. enabled={}, baseUrlConfigured={}, model={}, normalized={}, embeddingType={}, inputCount={}, inputLength={}",
                    properties.isEnabled(),
                    true,
                    request.getModel(),
                    request.getNormalized(),
                    request.getEmbeddingType(),
                    resolveInputCount(request.getInput()),
                    resolveInputLength(request.getInput()));
            ResponseEntity<EmbeddingResponse> response = restTemplate.postForEntity(
                    baseUrl,
                    entity,
                    EmbeddingResponse.class);
            EmbeddingResponse body = response.getBody();
            log.info("Embedding API response. model={}, statusCode={}, durationMs={}, dataCount={}",
                    request.getModel(),
                    response.getStatusCode().value(),
                    System.currentTimeMillis() - start,
                    body == null || body.getData() == null ? 0 : body.getData().size());
            return body;
        } catch (RestClientResponseException ex) {
            log.error("Embedding API HTTP error. enabled={}, baseUrlConfigured={}, model={}, normalized={}, embeddingType={}, statusCode={}, statusText={}, durationMs={}, error={}",
                    properties.isEnabled(),
                    true,
                    request.getModel(),
                    request.getNormalized(),
                    request.getEmbeddingType(),
                    ex.getStatusCode().value(),
                    LogSanitizer.safeMessage(ex.getStatusText()),
                    System.currentTimeMillis() - start,
                    LogSanitizer.safeMessage(ex.getMessage()),
                    ex);
            throw new BusinessException(50000,
                    "Embedding API 调用失败: HTTP " + ex.getStatusCode().value() + " " + ex.getStatusText());
        } catch (ResourceAccessException ex) {
            Throwable rootCause = rootCause(ex);
            String rootCauseMessage = safeRootCauseMessage(rootCause);
            log.error("Embedding API network or timeout error. enabled={}, baseUrlConfigured={}, model={}, normalized={}, embeddingType={}, durationMs={}, errorType={}, error={}, rootCauseType={}, rootCause={}",
                    properties.isEnabled(),
                    true,
                    request.getModel(),
                    request.getNormalized(),
                    request.getEmbeddingType(),
                    System.currentTimeMillis() - start,
                    ex.getClass().getSimpleName(),
                    LogSanitizer.safeMessage(ex.getMessage()),
                    rootCause == null ? null : rootCause.getClass().getName(),
                    rootCauseMessage,
                    ex);
            throw new BusinessException(50000, "Embedding API 调用失败: 网络或超时异常，原因: " + rootCauseMessage);
        } catch (HttpMessageConversionException ex) {
            log.error("Embedding API JSON conversion error. enabled={}, baseUrlConfigured={}, model={}, normalized={}, embeddingType={}, durationMs={}, errorType={}, error={}",
                    properties.isEnabled(),
                    true,
                    request.getModel(),
                    request.getNormalized(),
                    request.getEmbeddingType(),
                    System.currentTimeMillis() - start,
                    ex.getClass().getSimpleName(),
                    LogSanitizer.safeMessage(ex.getMessage()),
                    ex);
            throw new BusinessException(50000, "Embedding API 调用失败: JSON 解析失败");
        } catch (RestClientException ex) {
            log.error("Embedding API client error. enabled={}, baseUrlConfigured={}, model={}, normalized={}, embeddingType={}, durationMs={}, errorType={}, error={}",
                    properties.isEnabled(),
                    true,
                    request.getModel(),
                    request.getNormalized(),
                    request.getEmbeddingType(),
                    System.currentTimeMillis() - start,
                    ex.getClass().getSimpleName(),
                    LogSanitizer.safeMessage(ex.getMessage()),
                    ex);
            throw new BusinessException(50000, "Embedding API 调用失败");
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

    private int resolveInputCount(Object input) {
        if (input instanceof java.util.List<?> inputList) {
            return inputList.size();
        }
        return input == null ? 0 : 1;
    }

    private void rejectPlaceholderBaseUrl(String baseUrl) {
        if (baseUrl.toLowerCase().contains(EXAMPLE_HOST_MARKER)) {
            throw new BusinessException(50000, "Embedding baseUrl 未配置或仍为占位值，请设置 APP_EMBEDDING_BASE_URL");
        }
    }

    private int resolveInputLength(Object input) {
        if (input instanceof java.util.List<?> inputList) {
            return inputList.stream().mapToInt(this::singleInputLength).sum();
        }
        return singleInputLength(input);
    }

    private int singleInputLength(Object input) {
        if (input instanceof String text) {
            return text.length();
        }
        if (input instanceof EmbeddingInputItem item) {
            return item.getText() == null ? 0 : item.getText().length();
        }
        return 0;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String safeRootCauseMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getName();
        }
        return LogSanitizer.safeMessage(message);
    }
}
