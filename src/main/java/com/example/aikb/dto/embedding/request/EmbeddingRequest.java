package com.example.aikb.dto.embedding.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
     * 上游 input 字段。文本 embedding 模型使用字符串数组；调试多模态协议时可透传对象数组。
     */
    private Object input;

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
