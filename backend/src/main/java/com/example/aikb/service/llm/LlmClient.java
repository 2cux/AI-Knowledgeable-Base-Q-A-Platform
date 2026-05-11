package com.example.aikb.service.llm;

import java.util.List;

/**
 * LLM 客户端边界。后续替换具体模型供应商时，只需要替换该接口实现。
 */
public interface LlmClient {

    /**
     * 根据消息列表生成回答文本。
     *
     * @param messages prompt 消息列表
     * @return LLM 生成的回答
     */
    String chat(List<LlmMessage> messages);
}
