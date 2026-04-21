package com.example.aikb.service.llm.impl;

import com.example.aikb.config.AppLlmProperties;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.llm.LlmClient;
import com.example.aikb.service.llm.LlmMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OpenAI 兼容的 LLM HTTP 客户端实现。
 */
@Slf4j
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final int MAX_ERROR_LENGTH = 500;

    private final AppLlmProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleLlmClient(AppLlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
    }

    @Override
    public String chat(List<LlmMessage> messages) {
        validateConfig();
        if (messages == null || messages.isEmpty()) {
            throw new BusinessException("LLM prompt不能为空");
        }

        try {
            HttpRequest request = buildRequest(messages);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(50000, "LLM调用失败：" + parseErrorMessage(response.body()));
            }
            return parseAnswer(response.body());
        } catch (IOException ex) {
            log.warn("LLM request failed, model={}, error={}", properties.getModel(), ex.getMessage());
            throw new BusinessException(50000, "LLM服务请求失败");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(50000, "LLM服务请求被中断");
        }
    }

    private HttpRequest buildRequest(List<LlmMessage> messages) throws JsonProcessingException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("messages", messages);
        body.put("temperature", properties.getTemperature());
        body.put("max_tokens", properties.getMaxTokens());

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl()))
                .timeout(properties.getReadTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));

        if (StringUtils.hasText(properties.getApiKey())) {
            builder.header("Authorization", "Bearer " + properties.getApiKey().trim());
        }
        return builder.build();
    }

    private String parseAnswer(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        String answer = root.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText(null);
        if (!StringUtils.hasText(answer)) {
            throw new BusinessException(50000, "LLM返回内容为空");
        }
        return answer.trim();
    }

    private String parseErrorMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "响应为空";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = root.path("error").path("message").asText(responseBody);
            return truncate(message);
        } catch (JsonProcessingException ex) {
            return truncate(responseBody);
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new BusinessException(50000, "LLM服务地址未配置");
        }
        if (!StringUtils.hasText(properties.getModel())) {
            throw new BusinessException(50000, "LLM模型未配置");
        }
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_ERROR_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_LENGTH);
    }
}
