package com.example.aikb.service.llm.impl;

import com.example.aikb.dto.llm.request.LlmContentItem;
import com.example.aikb.dto.llm.request.LlmMessageRequest;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.llm.LlmClient;
import com.example.aikb.service.llm.LlmMessage;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 将项目现有 LlmClient 接口适配到第三方 LLM 协议。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VendorLlmClientAdapter implements LlmClient {

    private final LlmServiceImpl llmService;

    @Override
    public String chat(List<LlmMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new BusinessException("LLM messages 不能为空");
        }

        JsonNode rawResponse = llmService.chat(toRequestMessages(messages));
        return rawResponse == null ? "{}" : rawResponse.toString();
    }

    private List<LlmMessageRequest> toRequestMessages(List<LlmMessage> messages) {
        return messages.stream()
                .map(message -> LlmMessageRequest.builder()
                        .role(StringUtils.hasText(message.getRole()) ? message.getRole().trim() : "user")
                        .content(List.of(LlmContentItem.text(message.getContent())))
                        .build())
                .toList();
    }
}
