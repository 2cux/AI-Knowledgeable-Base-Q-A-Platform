package com.example.aikb.service.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM 对话消息，保持 OpenAI 兼容接口常用的 role/content 结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmMessage {

    /** 消息角色，例如 system、user、assistant。 */
    private String role;

    /** 消息内容。 */
    private String content;
}
