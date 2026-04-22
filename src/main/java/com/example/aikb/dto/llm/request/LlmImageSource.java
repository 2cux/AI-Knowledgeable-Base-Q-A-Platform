package com.example.aikb.dto.llm.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM 图片输入 source 对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmImageSource {

    /** 图片来源类型，当前协议要求为 base64。 */
    private String type;

    /** 图片媒体类型，对应 JSON 字段 media_type，例如 image/jpeg。 */
    @JsonProperty("media_type")
    private String mediaType;

    /** base64 图片数据。 */
    private String data;

    public static LlmImageSource base64(String mediaType, String data) {
        return LlmImageSource.builder()
                .type("base64")
                .mediaType(mediaType)
                .data(data)
                .build();
    }
}
