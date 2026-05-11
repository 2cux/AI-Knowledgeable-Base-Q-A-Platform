package com.example.aikb.dto.llm.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM message.content 数组中的单个内容项，支持 text 和 image 两种类型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LlmContentItem {

    /** 内容类型：text 或 image。 */
    private String type;

    /** 文本内容。type=text 时必填。 */
    private String text;

    /** 图片来源。type=image 时必填。 */
    private LlmImageSource source;

    public static LlmContentItem text(String text) {
        return LlmContentItem.builder()
                .type("text")
                .text(text)
                .build();
    }

    public static LlmContentItem imageBase64(String mediaType, String data) {
        return LlmContentItem.builder()
                .type("image")
                .source(LlmImageSource.base64(mediaType, data))
                .build();
    }
}
