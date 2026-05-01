package com.example.aikb.service.llm;

/**
 * Reason why an LLM raw response could not be converted into a final answer.
 */
public enum AnswerExtractFailureReason {

    EMPTY_RESPONSE,
    INVALID_JSON,
    MISSING_ANSWER_FIELD,
    EMPTY_ANSWER,
    UNSUPPORTED_STRUCTURE
}
