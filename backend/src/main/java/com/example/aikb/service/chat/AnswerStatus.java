package com.example.aikb.service.chat;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * RAG answer status for one chat ask request.
 */
public enum AnswerStatus {

    SUCCESS("SUCCESS"),
    NO_HIT("NO_HIT"),
    WEAK_HIT("WEAK_HIT"),
    LLM_UNAVAILABLE("LLM_UNAVAILABLE"),
    RETRIEVAL_UNAVAILABLE("RETRIEVAL_UNAVAILABLE");

    @EnumValue
    private final String value;

    AnswerStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
