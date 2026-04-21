package com.example.aikb.service.chat.impl;

import com.example.aikb.entity.ChatRecord;
import com.example.aikb.service.llm.LlmMessage;
import com.example.aikb.vo.retrieval.RetrievalChunkVO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * RAG 问答 prompt 构造器，集中维护知识片段、历史问答和回答约束。
 */
@Component
public class RagPromptBuilder {

    private static final int MAX_HISTORY_TEXT_LENGTH = 220;
    private static final int MAX_CHUNK_TEXT_LENGTH = 1200;

    public List<LlmMessage> build(String question, List<RetrievalChunkVO> chunks, List<ChatRecord> historyRecords) {
        List<LlmMessage> messages = new ArrayList<>();
        messages.add(LlmMessage.builder()
                .role("system")
                .content("""
                        你是企业 AI 知识库问答助手。
                        必须优先依据用户提供的知识库片段回答。
                        如果知识片段不足以回答问题，请明确说明“根据当前知识库资料，我不知道”或“未找到足够信息”，不要编造。
                        回答要简洁、清晰，并尽量使用中文。
                        """)
                .build());
        messages.add(LlmMessage.builder()
                .role("user")
                .content(buildUserPrompt(question, chunks, historyRecords))
                .build());
        return messages;
    }

    private String buildUserPrompt(String question, List<RetrievalChunkVO> chunks, List<ChatRecord> historyRecords) {
        StringBuilder prompt = new StringBuilder();
        appendHistory(prompt, historyRecords);
        appendChunks(prompt, chunks);
        prompt.append("\n用户问题：\n").append(question).append("\n\n");
        prompt.append("请基于以上知识片段回答。如果片段无法支持答案，请明确说明不知道。");
        return prompt.toString();
    }

    private void appendHistory(StringBuilder prompt, List<ChatRecord> historyRecords) {
        if (historyRecords == null || historyRecords.isEmpty()) {
            return;
        }
        prompt.append("最近历史问答：\n");
        for (int i = 0; i < historyRecords.size(); i++) {
            ChatRecord record = historyRecords.get(i);
            prompt.append(i + 1)
                    .append(". 问：")
                    .append(shorten(record.getQuestion(), MAX_HISTORY_TEXT_LENGTH))
                    .append("\n   答：")
                    .append(shorten(record.getAnswer(), MAX_HISTORY_TEXT_LENGTH))
                    .append("\n");
        }
        prompt.append("\n");
    }

    private void appendChunks(StringBuilder prompt, List<RetrievalChunkVO> chunks) {
        prompt.append("知识库片段：\n");
        for (int i = 0; i < chunks.size(); i++) {
            RetrievalChunkVO chunk = chunks.get(i);
            prompt.append("[来源")
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
