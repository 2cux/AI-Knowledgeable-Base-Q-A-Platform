package com.example.aikb.dto.llm.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM 请求中的单条 message。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmMessageRequest {

    /** 消息角色，例如 user、assistant、system。 */
    private String role;

    /** 内容数组。纯文本场景也必须使用数组结构。 */
    private List<LlmContentItem> content;

    public static LlmMessageRequest userText(String text) {
        return LlmMessageRequest.builder()
                .role("user")
                .content(List.of(LlmContentItem.text(text)))
                .build();
    }
}
