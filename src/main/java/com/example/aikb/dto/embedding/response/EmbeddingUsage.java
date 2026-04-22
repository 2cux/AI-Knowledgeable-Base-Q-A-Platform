package com.example.aikb.dto.embedding.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * embedding 接口返回的 Token 用量。
 */
@Data
public class EmbeddingUsage {

    /**
     * 请求输入消耗的 Token 数，对应 JSON 字段 prompt_tokens。
     */
    @JsonProperty("prompt_tokens")
    private Integer promptTokens;

    /**
     * 本次请求总 Token 数，对应 JSON 字段 total_tokens。
     */
    @JsonProperty("total_tokens")
    private Integer totalTokens;
}
