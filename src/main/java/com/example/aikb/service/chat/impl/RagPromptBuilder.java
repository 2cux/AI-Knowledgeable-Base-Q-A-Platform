package com.example.aikb.service.chat.impl;

import com.example.aikb.service.llm.LlmMessage;
import com.example.aikb.vo.retrieval.RetrievalChunkVO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds the RAG prompt from retrieved chunks and conversation context.
 */
@Component
public class RagPromptBuilder {

    private static final int MAX_CHUNK_TEXT_LENGTH = 1200;

    public List<LlmMessage> build(String question, List<RetrievalChunkVO> chunks, String conversationContext) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.builder()
                .role("system")
                .content("""
                        You are an enterprise knowledge-base Q&A assistant.
                        Answer primarily from the provided knowledge-base chunks.
                        If the chunks do not support an answer, say that the current knowledge base has insufficient information.
                        Keep the answer concise and prefer Chinese when the user asks in Chinese.
                        """)
                .build());
        messages.add(LlmMessage.builder()
                .role("user")
                .content(buildUserPrompt(question, chunks, conversationContext))
                .build());
        return messages;
    }

    private String buildUserPrompt(String question, List<RetrievalChunkVO> chunks, String conversationContext) {
        StringBuilder prompt = new StringBuilder();
        appendHistory(prompt, conversationContext);
        appendChunks(prompt, chunks);
        prompt.append("\nUser question:\n").append(question).append("\n\n");
        prompt.append("Please answer based on the knowledge-base chunks above. ");
        prompt.append("If the chunks cannot support the answer, clearly say you do not know.");
        return prompt.toString();
    }

    private void appendHistory(StringBuilder prompt, String conversationContext) {
        if (conversationContext == null || conversationContext.isBlank()) {
            return;
        }
        prompt.append(conversationContext).append("\n");
    }

    private void appendChunks(StringBuilder prompt, List<RetrievalChunkVO> chunks) {
        prompt.append("Knowledge-base chunks:\n");
        for (int i = 0; i < chunks.size(); i++) {
            RetrievalChunkVO chunk = chunks.get(i);
            prompt.append("[Source ")
                    .append(i + 1)
                    .append("] document: ")
                    .append(chunk.getDocumentName() == null ? "unknown" : chunk.getDocumentName())
                    .append(", chunkIndex: ")
                    .append(chunk.getChunkIndex())
                    .append(", score: ")
                    .append(chunk.getScore())
                    .append("\n")
                    .append(shorten(chunk.getContent(), MAX_CHUNK_TEXT_LENGTH))
                    .append("\n\n");
        }
    }

    private String shorten(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
