package com.example.aikb.service.chat.impl;

import com.example.aikb.service.chat.AnswerGeneratorService;
import com.example.aikb.vo.retrieval.RetrievalChunkVO;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * 占位答案生成实现。没有接入真实LLM前，先把检索上下文拼成可读答案，保证主链路可运行。
 */
@Service
@ConditionalOnMissingBean(AnswerGeneratorService.class)
public class SimpleAnswerGeneratorServiceImpl implements AnswerGeneratorService {

    @Override
    public String generate(String question, List<RetrievalChunkVO> chunks) {
        StringBuilder answer = new StringBuilder();
        answer.append("以下是基于检索上下文生成的占位答案，后续可替换为真实大模型回答。");
        answer.append("问题：").append(question).append("\n\n");
        answer.append("相关上下文：\n");

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
