package com.example.aikb.service.llm;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Result of extracting a business answer from an LLM raw response.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AnswerExtractResult {

    private final boolean success;

    private final String answer;

    private final AnswerExtractFailureReason failureReason;

    public static AnswerExtractResult success(String answer) {
        return new AnswerExtractResult(true, answer, null);
    }

    public static AnswerExtractResult failure(AnswerExtractFailureReason failureReason) {
        return new AnswerExtractResult(false, null, failureReason);
    }
}
