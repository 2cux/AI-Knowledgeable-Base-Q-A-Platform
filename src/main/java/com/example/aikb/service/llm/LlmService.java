package com.example.aikb.service.llm;

import com.example.aikb.dto.llm.request.LlmMessageRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * 最小 LLM 服务示例，面向业务代码暴露文本问答入口。
 */
public interface LlmService {

    /**
     * 使用配置中的默认模型发送纯文本问题。
     *
     * @param userQuestion 用户问题
     * @return 原始 JSON 响应
     */
    JsonNode chatText(String userQuestion);

    /**
     * 使用指定 messages 调用 LLM。
     *
     * @param messages 符合第三方协议的消息数组
     * @return 原始 JSON 响应
     */
    JsonNode chat(List<LlmMessageRequest> messages);
}
