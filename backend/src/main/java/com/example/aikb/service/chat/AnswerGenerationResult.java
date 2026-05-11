package com.example.aikb.service.chat;

import lombok.Builder;
import lombok.Data;

/**
 * Result returned by the answer generator after attempting an LLM call.
 */
@Data
@Builder
public class AnswerGenerationResult {

    private String answer;

    private boolean llmAvailable;
}
