package com.example.aikb.service.chat.impl;

import com.example.aikb.common.LogSanitizer;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.chat.AnswerGenerationResult;
import com.example.aikb.service.chat.AnswerGeneratorService;
import com.example.aikb.service.llm.AnswerExtractResult;
import com.example.aikb.service.llm.AnswerExtractor;
import com.example.aikb.service.llm.LlmClient;
import com.example.aikb.service.llm.LlmMessage;
import com.example.aikb.vo.retrieval.RetrievalChunkVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final AnswerExtractor answerExtractor;

    @Override
    public AnswerGenerationResult generate(String question, List<RetrievalChunkVO> chunks,
            String conversationContext) {
        if (chunks == null || chunks.isEmpty()) {
            return unavailable();
        }

        long t0 = System.currentTimeMillis();
        try {
            List<LlmMessage> messages = ragPromptBuilder.build(question, chunks, conversationContext);
            long t1 = System.currentTimeMillis();
            long promptBuildCost = t1 - t0;

            String rawResponse = llmClient.chat(messages);
            long t2 = System.currentTimeMillis();
            long llmCallCost = t2 - t1;

            log.info("LLM raw response received before extraction, questionLength={}, chunkCount={}, outputLength={}, promptBuildCost={}ms, llmCallCost={}ms",
                    question == null ? 0 : question.length(),
                    chunks.size(),
                    rawResponse == null ? 0 : rawResponse.length(),
                    promptBuildCost, llmCallCost);

            AnswerExtractResult extractResult = answerExtractor.extract(rawResponse);
            long t3 = System.currentTimeMillis();
            long responseParseCost = t3 - t2;

            if (!extractResult.isSuccess()) {
                log.warn("[RAG-TIME] promptBuildCost={}ms, llmCallCost={}ms, responseParseCost={}ms, totalGenerationCost={}ms, questionLength={}, chunkCount={}, result=EXTRACTION_FAILED, failureReason={}",
                        promptBuildCost, llmCallCost, responseParseCost, t3 - t0,
                        question == null ? 0 : question.length(), chunks.size(),
                        extractResult.getFailureReason());
                return unavailable();
            }

            log.info("[RAG-TIME] promptBuildCost={}ms, llmCallCost={}ms, responseParseCost={}ms, totalGenerationCost={}ms, questionLength={}, chunkCount={}, result=SUCCESS",
                    promptBuildCost, llmCallCost, responseParseCost, t3 - t0,
                    question == null ? 0 : question.length(), chunks.size());
            return AnswerGenerationResult.builder()
                    .answer(extractResult.getAnswer())
                    .llmAvailable(true)
                    .build();
        } catch (BusinessException ex) {
            log.warn("[RAG-TIME] promptBuildCost={}ms, totalGenerationCost={}ms, questionLength={}, chunkCount={}, result=LLM_FAILED, error={}",
                    System.currentTimeMillis() - t0, System.currentTimeMillis() - t0,
                    question == null ? 0 : question.length(), chunks.size(),
                    LogSanitizer.safeMessage(ex.getMessage()));
            return unavailable();
        } catch (RuntimeException ex) {
            log.warn("[RAG-TIME] promptBuildCost={}ms, totalGenerationCost={}ms, questionLength={}, chunkCount={}, result=LLM_FAILED, errorType={}",
                    System.currentTimeMillis() - t0, System.currentTimeMillis() - t0,
                    question == null ? 0 : question.length(), chunks.size(),
                    ex.getClass().getSimpleName());
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
