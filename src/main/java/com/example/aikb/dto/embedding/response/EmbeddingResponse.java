package com.example.aikb.dto.embedding.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/**
 * 通用可扩展的 embedding 响应。
 *
 * <p>data、usage、error 的结构需要在确认服务提供商真实响应后再调整。</p>
 */
@Data
public class EmbeddingResponse {

    private String id;

    private String object;

    private String model;

    /**
     * 常见的 embedding 响应字段，部分服务提供商可能命名为 embeddings 或 results。
     */
    @JsonAlias({"embeddings", "results"})
    private List<EmbeddingData> data;

    /**
     * 兼容部分服务提供商在响应根节点直接返回单个向量的情况。
     */
    private List<Float> embedding;

    private EmbeddingUsage usage;

    private EmbeddingError error;

    /**
     * 可选的请求 ID。如果真实字段名不同，需要按实际响应调整。
     */
    @JsonProperty("request_id")
    private String requestId;
}
