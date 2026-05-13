package com.example.aikb.service.llm.impl;

import com.example.aikb.config.AppLlmProperties;
import com.example.aikb.dto.llm.request.LlmMessageRequest;
import com.example.aikb.dto.llm.request.LlmRequest;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.llm.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 最小 LLM 服务实现，负责组装请求体并保留返回原始 JSON。
 */
@Service
@RequiredArgsConstructor
public class LlmServiceImpl implements LlmService {

    private static final String EXAMPLE_HOST_MARKER = "example.com";
    private static final String LLM_PLACEHOLDER_MODEL = "your-chat-model";

    private final AppLlmProperties properties;
    private final LlmApiClient llmApiClient;

    @Override
    public JsonNode chatText(String userQuestion) {
        validateQuestion(userQuestion);
        return chat(List.of(LlmMessageRequest.userText(userQuestion.trim())));
    }

    @Override
    public JsonNode chat(List<LlmMessageRequest> messages) {
        validateConfig();
        if (messages == null || messages.isEmpty()) {
            throw new BusinessException("LLM messages 不能为空");
        }

        LlmRequest request = LlmRequest.builder()
                .model(properties.getModel())
                .maxTokens(properties.getMaxTokens())
                .messages(messages)
                .build();
        return llmApiClient.chat(request);
    }

    private void validateQuestion(String userQuestion) {
        if (!StringUtils.hasText(userQuestion)) {
            throw new BusinessException("用户问题不能为空");
        }
    }

    private void validateConfig() {
        if (!properties.isEnabled()) {
            throw new BusinessException(50000, "LLM 调用未启用");
        }
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new BusinessException(50000, "LLM base-url 未配置");
        }
        if (properties.getBaseUrl().toLowerCase().contains(EXAMPLE_HOST_MARKER)) {
            throw new BusinessException(50000, "LLM baseUrl 未配置或仍为占位值，请设置 APP_LLM_BASE_URL");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException(50000,
                    "LLM api-key 未配置，请通过环境变量 APP_LLM_API_KEY 或 OPENAI_API_KEY 注入");
        }
        if (!StringUtils.hasText(properties.getModel())) {
            throw new BusinessException(50000, "LLM model 未配置");
        }
        if (LLM_PLACEHOLDER_MODEL.equalsIgnoreCase(properties.getModel().trim())) {
            throw new BusinessException(50000, "LLM model 仍为占位值，请设置 APP_LLM_MODEL");
        }
    }
}
