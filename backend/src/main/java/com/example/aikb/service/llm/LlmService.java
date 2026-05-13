package com.example.aikb.service.llm;

import com.example.aikb.dto.llm.request.LlmMessageRequest;
import java.util.List;

/**
 * Minimal text QA entry point for the external LLM service.
 */
public interface LlmService {

    /**
     * Sends a plain text question with the configured default model.
     *
     * @param userQuestion user question
     * @return raw JSON response body
     */
    String chatText(String userQuestion);

    /**
     * Sends messages to the configured LLM.
     *
     * @param messages messages compatible with the project LLM boundary
     * @return raw JSON response body
     */
    String chat(List<LlmMessageRequest> messages);
}
