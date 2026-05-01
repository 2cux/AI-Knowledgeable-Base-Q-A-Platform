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
                        你是企业 AI 知识库问答助手。
                        必须优先依据用户提供的知识库片段回答。
                        如果知识库片段不足以回答问题，请明确说明当前知识库资料不足，不要编造。
                        回答要简洁、清晰，并尽量使用中文。
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
        prompt.append("\n用户问题：\n").append(question).append("\n\n");
        prompt.append("请基于以上知识库片段回答。");
        prompt.append("如果片段无法支持答案，请明确说明不知道。");
        return prompt.toString();
    }

    private void appendHistory(StringBuilder prompt, String conversationContext) {
        if (conversationContext == null || conversationContext.isBlank()) {
            return;
        }
        prompt.append(conversationContext).append("\n");
    }

    private void appendChunks(StringBuilder prompt, List<RetrievalChunkVO> chunks) {
        prompt.append("知识库片段：\n");
        for (int i = 0; i < chunks.size(); i++) {
            RetrievalChunkVO chunk = chunks.get(i);
            prompt.append("[Source ")
                    .append(i + 1)
                    .append("] 文档：")
                    .append(chunk.getDocumentName() == null ? "未知文档" : chunk.getDocumentName())
                    .append("，chunkIndex：")
                    .append(chunk.getChunkIndex())
                    .append("，score：")
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
