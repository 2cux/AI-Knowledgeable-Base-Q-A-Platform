package com.example.aikb.service.embedding.impl;

import com.example.aikb.config.AppEmbeddingProperties;
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
 * embedding 第三方接口基础客户端，只负责 HTTP 请求发送和响应反序列化。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.embedding", name = "enabled", havingValue = "true")
public class EmbeddingApiClient {

    private static final int LOG_BODY_LIMIT = 2000;

    private final AppEmbeddingProperties properties;
    private final RestTemplate restTemplate;
    public EmbeddingApiClient(
            AppEmbeddingProperties properties,
            @Qualifier("embeddingRestTemplate") RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    /**
     * 按指定协议调用 embedding 接口。
     *
     * <p>请求头固定为 Authorization: Bearer {API_KEY}，Content-Type 固定为 application/json。</p>
     *
     * <p>API Key 必须来自环境变量注入，日志和响应中都不得输出真实密钥。</p>
     */
    public EmbeddingResponse embed(EmbeddingRequest request) {
        String baseUrl = requireText(properties.getBaseUrl(), "embedding base-url 未配置");
        String apiKey = requireText(properties.getApiKey(),
                "embedding api-key 未配置，请通过环境变量 APP_EMBEDDING_API_KEY 或 OPENAI_API_KEY 注入");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(request, headers);
        try {
            log.info(
                    "Embedding API request. enabled={}, baseUrl={}, model={}, normalized={}, embeddingType={}, inputCount={}",
                    properties.isEnabled(),
                    baseUrl,
                    request.getModel(),
                    properties.getNormalized(),
                    properties.getEmbeddingType(),
                    resolveInputCount(request.getInput()));
            ResponseEntity<EmbeddingResponse> response = restTemplate.postForEntity(
                    baseUrl,
                    entity,
                    EmbeddingResponse.class);
            return response.getBody();
        } catch (RestClientResponseException ex) {
            log.error(
                    "Embedding API HTTP error. enabled={}, baseUrl={}, model={}, normalized={}, embeddingType={}, "
                            + "statusCode={}, statusText={}",
                    properties.isEnabled(),
                    baseUrl,
                    request.getModel(),
                    properties.getNormalized(),
                    properties.getEmbeddingType(),
                    ex.getStatusCode().value(),
                    ex.getStatusText(),
                    ex);
            throw new BusinessException(50000,
                    "Embedding API 调用失败: HTTP " + ex.getStatusCode().value() + " " + ex.getStatusText());
        } catch (ResourceAccessException ex) {
            log.error(
                    "Embedding API network or timeout error. enabled={}, baseUrl={}, model={}, normalized={}, "
                            + "embeddingType={}, error={}",
                    properties.isEnabled(),
                    baseUrl,
                    request.getModel(),
                    properties.getNormalized(),
                    properties.getEmbeddingType(),
                    ex.getMessage(),
                    ex);
            throw new BusinessException(50000, "Embedding API 调用失败: 网络或超时异常，" + safeMessage(ex));
        } catch (HttpMessageConversionException ex) {
            log.error(
                    "Embedding API JSON conversion error. enabled={}, baseUrl={}, model={}, normalized={}, "
                            + "embeddingType={}, error={}",
                    properties.isEnabled(),
                    baseUrl,
                    request.getModel(),
                    properties.getNormalized(),
                    properties.getEmbeddingType(),
                    ex.getMessage(),
                    ex);
            throw new BusinessException(50000, "Embedding API 调用失败: JSON 解析失败，" + safeMessage(ex));
        } catch (RestClientException ex) {
            log.error(
                    "Embedding API client error. enabled={}, baseUrl={}, model={}, normalized={}, embeddingType={}, "
                            + "error={}",
                    properties.isEnabled(),
                    baseUrl,
                    request.getModel(),
                    properties.getNormalized(),
                    properties.getEmbeddingType(),
                    ex.getMessage(),
                    ex);
            throw new BusinessException(50000, "Embedding API 调用失败: " + safeMessage(ex));
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

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return truncate(message);
    }

    private int resolveInputCount(Object input) {
        if (input instanceof java.util.List<?> inputList) {
            return inputList.size();
        }
        return input == null ? 0 : 1;
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= LOG_BODY_LIMIT) {
            return value;
        }
        return value.substring(0, LOG_BODY_LIMIT) + "...(truncated)";
    }
}
