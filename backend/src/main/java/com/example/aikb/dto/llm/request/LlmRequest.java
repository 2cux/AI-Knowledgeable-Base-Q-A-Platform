package com.example.aikb.dto.llm.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 第三方 LLM API 请求体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRequest {

    /** 模型名称。 */
    private String model;

    /** 最大输出 token 数，对应 JSON 字段 max_tokens。 */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /** 消息数组。 */
    private List<LlmMessageRequest> messages;
}
