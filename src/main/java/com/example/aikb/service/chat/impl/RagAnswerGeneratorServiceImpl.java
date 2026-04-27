package com.example.aikb.service.chat.impl;

import com.example.aikb.entity.ChatRecord;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.chat.AnswerGenerationResult;
import com.example.aikb.service.chat.AnswerGeneratorService;
import com.example.aikb.service.llm.LlmClient;
import com.example.aikb.vo.retrieval.RetrievalChunkVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Real RAG answer generator based on retrieved chunks and LLM calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagAnswerGeneratorServiceImpl implements AnswerGeneratorService {

    private static final String LLM_FAILED_ANSWER =
            "\u5df2\u68c0\u7d22\u5230\u76f8\u5173\u77e5\u8bc6\u7247\u6bb5\uff0c"
                    + "\u4f46 LLM \u670d\u52a1\u6682\u65f6\u4e0d\u53ef\u7528\uff0c"
                    + "\u65e0\u6cd5\u751f\u6210\u53ef\u9760\u56de\u7b54\u3002"
                    + "\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002";

    private final RagPromptBuilder ragPromptBuilder;
    private final LlmClient llmClient;

    @Override
    public AnswerGenerationResult generate(String question, List<RetrievalChunkVO> chunks,
            List<ChatRecord> historyRecords) {
        if (chunks == null || chunks.isEmpty()) {
            return unavailable();
        }

        try {
            String answer = llmClient.chat(ragPromptBuilder.build(question, chunks, historyRecords));
            if (!StringUtils.hasText(answer)) {
                log.warn("LLM answer generation returned empty answer, questionLength={}, chunkCount={}",
                        question == null ? 0 : question.length(), chunks.size());
                return unavailable();
            }
            return AnswerGenerationResult.builder()
                    .answer(answer.trim())
                    .llmAvailable(true)
                    .build();
        } catch (BusinessException ex) {
            log.warn("LLM answer generation failed, questionLength={}, chunkCount={}, error={}",
                    question == null ? 0 : question.length(), chunks.size(), ex.getMessage());
            return unavailable();
        } catch (RuntimeException ex) {
            log.warn("LLM answer generation failed unexpectedly, questionLength={}, chunkCount={}, errorType={}",
                    question == null ? 0 : question.length(), chunks.size(), ex.getClass().getSimpleName());
            return unavailable();
        }
    }

    private AnswerGenerationResult unavailable() {
        return AnswerGenerationResult.builder()
                .answer(LLM_FAILED_ANSWER)
                .llmAvailable(false)
                .build();
    }
}
