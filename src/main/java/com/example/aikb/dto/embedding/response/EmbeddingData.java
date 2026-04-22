package com.example.aikb.dto.embedding.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/**
 * 单条 embedding 结果。若服务提供商返回结构不同，需要按实际响应调整字段。
 */
@Data
public class EmbeddingData {

    private Integer index;

    private String object;

    /**
     * 服务提供商返回的向量。
     */
    @JsonAlias({"vector"})
    private List<Float> embedding;

    /**
     * 可选的服务提供商自定义 embedding 类型，实际响应不同时需要调整。
     */
    @JsonProperty("embedding_type")
    private String embeddingType;
}
