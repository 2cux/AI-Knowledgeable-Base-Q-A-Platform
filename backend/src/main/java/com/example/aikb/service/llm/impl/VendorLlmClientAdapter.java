package com.example.aikb.service.llm.impl;

import com.example.aikb.dto.llm.request.LlmMessageRequest;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.llm.LlmClient;
import com.example.aikb.service.llm.LlmMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Adapts the project LlmClient boundary to the third-party LLM protocol.
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

        return llmService.chat(toRequestMessages(messages));
    }

    private List<LlmMessageRequest> toRequestMessages(List<LlmMessage> messages) {
        return messages.stream()
                .map(message -> LlmMessageRequest.builder()
                        .role(StringUtils.hasText(message.getRole()) ? message.getRole().trim() : "user")
                        .content(message.getContent())
                        .build())
                .toList();
    }
}
