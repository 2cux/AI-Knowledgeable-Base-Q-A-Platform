package com.example.aikb.dto.embedding.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * embedding API 的单个输入项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingInputItem {

    /**
     * 需要向量化的文本。
     */
    private String text;

    /**
     * 图片内容或图片地址。纯文本调用时按上游协议发送为空字符串。
     */
    private String image;

    public static EmbeddingInputItem textOnly(String text) {
        return textAndImage(text, null);
    }

    public static EmbeddingInputItem textAndImage(String text, String image) {
        return EmbeddingInputItem.builder()
                .text(text)
                .image(image == null ? "" : image.trim())
                .build();
    }
}
