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
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * embedding 第三方接口基础客户端，只负责 HTTP 请求发送和响应反序列化。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.embedding", name = "enabled", havingValue = "true")
public class EmbeddingApiClient {

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
     * <p>请求头固定为 Authorization: Bearer {API_KEY}，
     * Content-Type 固定为 application/json。</p>
     *
     * @param request embedding 请求体
     * @return embedding 响应体
     */
    public EmbeddingResponse embed(EmbeddingRequest request) {
        String baseUrl = requireText(properties.getBaseUrl(), "embedding base-url 未配置");
        String apiKey = requireText(properties.getApiKey(), "embedding api-key 未配置");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<EmbeddingResponse> response = restTemplate.postForEntity(
                    baseUrl,
                    entity,
                    EmbeddingResponse.class);
            return response.getBody();
        } catch (RestClientException ex) {
            log.warn("Embedding API 调用失败，model={}，error={}", request.getModel(), ex.getMessage());
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
}
