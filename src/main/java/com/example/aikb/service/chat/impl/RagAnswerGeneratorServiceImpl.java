package com.example.aikb.service.chat.impl;

import com.example.aikb.exception.BusinessException;
import com.example.aikb.entity.ChatRecord;
import com.example.aikb.service.chat.AnswerGeneratorService;
import com.example.aikb.service.llm.LlmClient;
import com.example.aikb.vo.retrieval.RetrievalChunkVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 真实 RAG 答案生成实现：基于检索片段构造 prompt，并调用 LLM 生成答案。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagAnswerGeneratorServiceImpl implements AnswerGeneratorService {

    private static final String NO_CONTEXT_ANSWER =
            "根据当前知识库资料，我没有找到足够相关的内容，暂时无法回答这个问题。";
    private static final String LLM_FAILED_ANSWER =
            "已检索到相关知识片段，但 LLM 服务暂时不可用，无法生成可靠回答。请稍后重试。";

    private final RagPromptBuilder ragPromptBuilder;
    private final LlmClient llmClient;

    @Override
    public String generate(String question, List<RetrievalChunkVO> chunks, List<ChatRecord> historyRecords) {
        if (chunks == null || chunks.isEmpty()) {
            return NO_CONTEXT_ANSWER;
        }

        try {
            String answer = llmClient.chat(ragPromptBuilder.build(question, chunks, historyRecords));
            if (!StringUtils.hasText(answer)) {
                return NO_CONTEXT_ANSWER;
            }
            return answer.trim();
        } catch (BusinessException ex) {
            log.warn("LLM answer generation failed, questionLength={}, chunkCount={}, error={}",
                    question == null ? 0 : question.length(), chunks.size(), ex.getMessage());
            return LLM_FAILED_ANSWER;
        }
    }
}
