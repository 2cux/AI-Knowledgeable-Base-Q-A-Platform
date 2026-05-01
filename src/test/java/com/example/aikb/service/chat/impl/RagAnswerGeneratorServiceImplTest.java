package com.example.aikb.service.chat.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.aikb.service.chat.AnswerGenerationResult;
import com.example.aikb.service.llm.AnswerExtractor;
import com.example.aikb.service.llm.LlmClient;
import com.example.aikb.vo.retrieval.RetrievalChunkVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RagAnswerGeneratorServiceImplTest {

    private LlmClient llmClient;
    private RagAnswerGeneratorServiceImpl service;

    @BeforeEach
    void setUp() {
        llmClient = mock(LlmClient.class);
        service = new RagAnswerGeneratorServiceImpl(new RagPromptBuilder(), llmClient,
                new AnswerExtractor(new ObjectMapper()));
    }

    @Test
    void shouldReturnExtractedAnswerFromRawLlmResponse() {
        when(llmClient.chat(anyList())).thenReturn("{\"answer\":\"  extracted answer  \"}");

        AnswerGenerationResult result = service.generate("question", List.of(chunk()), List.of());

        assertThat(result.isLlmAvailable()).isTrue();
        assertThat(result.getAnswer()).isEqualTo("extracted answer");
    }

    @Test
    void shouldFallbackWhenAnswerExtractionFails() {
        when(llmClient.chat(anyList())).thenReturn("{\"answer\":\"   \"}");

        AnswerGenerationResult result = service.generate("question", List.of(chunk()), List.of());

        assertThat(result.isLlmAvailable()).isFalse();
        assertThat(result.getAnswer()).contains("LLM");
    }

    private RetrievalChunkVO chunk() {
        return RetrievalChunkVO.builder()
                .chunkId(1L)
                .content("chunk content")
                .score(0.9)
                .build();
    }
}
