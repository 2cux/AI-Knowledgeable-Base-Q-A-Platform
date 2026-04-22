package com.example.aikb.service.llm.impl;

import com.example.aikb.config.AppLlmProperties;
import com.example.aikb.dto.llm.request.LlmRequest;
import com.example.aikb.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
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
 * LLM 第三方接口基础客户端，只负责 HTTP 请求发送和原始 JSON 响应接收。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmApiClient {

    private final AppLlmProperties properties;

    @Qualifier("llmRestTemplate")
    private final RestTemplate restTemplate;

    /**
     * 按指定协议调用 LLM 接口。
     *
     * <p>请求头固定为 Authorization: Bearer {API_KEY}，
     * Content-Type 固定为 application/json。</p>
     *
     * @param request LLM 请求体
     * @return 原始 JSON 响应
     */
    public JsonNode chat(LlmRequest request) {
        String baseUrl = requireText(properties.getBaseUrl(), "LLM base-url 未配置");
        String apiKey = requireText(properties.getApiKey(), "LLM api-key 未配置");

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LlmRequest> entity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(baseUrl, entity, JsonNode.class);
            return response.getBody();
        } catch (RestClientException ex) {
            log.warn("LLM API 调用失败，model={}，error={}", request.getModel(), ex.getMessage());
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
}
