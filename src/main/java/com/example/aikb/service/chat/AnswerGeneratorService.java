package com.example.aikb.service.chat;

import com.example.aikb.entity.ChatRecord;
import com.example.aikb.vo.retrieval.RetrievalChunkVO;
import java.util.List;

/**
 * 答案生成服务边界。MVP阶段由占位实现返回简单答案，后续可替换为真实LLM调用。
 */
public interface AnswerGeneratorService {

    /**
     * 根据用户问题和检索到的上下文切片生成答案。
     *
     * @param question 用户问题
     * @param chunks 检索命中的上下文切片
     * @return 生成的答案
     */
    String generate(String question, List<RetrievalChunkVO> chunks, List<ChatRecord> historyRecords);
}
