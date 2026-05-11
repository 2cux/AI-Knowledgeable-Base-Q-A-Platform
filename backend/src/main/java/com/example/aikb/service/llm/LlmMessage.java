package com.example.aikb.service.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目内部 LLM 对话消息，由适配器转换为第三方接口要求的 messages/content[] 结构。
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
