package com.example.aikb.dto.llm.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Claude Messages request body for the third-party LLM API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRequest {

    /** Model name. */
    private String model;

    /** Maximum output tokens, serialized as max_tokens. */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /** Claude top-level system prompt. */
    private String system;

    /** Messages sent to Claude. */
    private List<LlmMessageRequest> messages;
}
