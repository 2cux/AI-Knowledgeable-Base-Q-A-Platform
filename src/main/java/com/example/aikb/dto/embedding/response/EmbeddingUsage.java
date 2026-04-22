package com.example.aikb.dto.embedding.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Token 用量信息。字段名可能需要根据服务提供商响应调整。
 */
@Data
public class EmbeddingUsage {

    @JsonProperty("prompt_tokens")
    @JsonAlias({"input_tokens"})
    private Integer promptTokens;

    @JsonProperty("total_tokens")
    private Integer totalTokens;
}
