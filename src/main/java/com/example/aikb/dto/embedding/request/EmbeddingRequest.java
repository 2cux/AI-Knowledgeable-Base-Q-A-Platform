package com.example.aikb.dto.embedding.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 外部 embedding API 请求体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequest {

    /**
     * embedding 服务提供商使用的模型名称。
     */
    private String model;

    /**
     * 批量输入列表。接口协议要求该字段为数组。
     */
    private List<EmbeddingInputItem> input;

    /**
     * 是否要求服务提供商返回归一化后的向量。
     */
    private Boolean normalized;

    /**
     * JSON 字段名为 embedding_type，Java 字段保持 camelCase 命名。
     */
    @JsonProperty("embedding_type")
    private String embeddingType;
}
