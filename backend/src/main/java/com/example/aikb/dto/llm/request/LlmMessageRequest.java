package com.example.aikb.dto.llm.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Claude Messages API message item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmMessageRequest {

    /** Message role, for example user or assistant. */
    private String role;

    /** Text content for current RAG question answering. */
    private String content;

    public static LlmMessageRequest userText(String text) {
        return LlmMessageRequest.builder()
                .role("user")
                .content(text)
                .build();
    }
}
