package com.example.aikb.service.chat.impl;

import com.example.aikb.entity.ChatRecord;
import com.example.aikb.service.chat.AnswerGeneratorService;
import com.example.aikb.vo.retrieval.RetrievalChunkVO;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * 占位答案生成实现。后续接入真实 LLM 时，只需要提供新的 AnswerGeneratorService 实现，
 * 将历史问答、当前问题和检索上下文组装为 prompt 后调用模型即可。
 */
@Service
@ConditionalOnMissingBean(AnswerGeneratorService.class)
public class SimpleAnswerGeneratorServiceImpl implements AnswerGeneratorService {

    @Override
    public String generate(String question, List<RetrievalChunkVO> chunks, List<ChatRecord> historyRecords) {
        StringBuilder answer = new StringBuilder();
        answer.append("以下是基于历史问答和检索上下文生成的占位答案，后续可替换为真实大模型调用。\n\n");
        answer.append("当前问题：").append(question).append("\n\n");

        if (historyRecords != null && !historyRecords.isEmpty()) {
            answer.append("最近历史问答：\n");
            for (int i = 0; i < historyRecords.size(); i++) {
                ChatRecord record = historyRecords.get(i);
                answer.append(i + 1)
                        .append(". 问：")
                        .append(shorten(record.getQuestion(), 120))
                        .append("\n   答：")
                        .append(shorten(record.getAnswer(), 180))
                        .append("\n");
            }
            answer.append("\n");
        }

        if (chunks == null || chunks.isEmpty()) {
            answer.append("当前问题未检索到新的相关知识库内容，请补充或向量化相关文档后再试。");
            return answer.toString().trim();
        }

        answer.append("当前检索上下文：\n");
        for (int i = 0; i < chunks.size(); i++) {
            RetrievalChunkVO chunk = chunks.get(i);
            answer.append(i + 1)
                    .append(". [")
                    .append(chunk.getDocumentName() == null ? "未知文档" : chunk.getDocumentName())
                    .append(" #")
                    .append(chunk.getChunkIndex())
                    .append("] ")
                    .append(shorten(chunk.getContent(), 220))
                    .append("\n");
        }
        return answer.toString().trim();
    }

    private String shorten(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
