package com.example.aikb.service.embedding;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 单个 chunk 的向量化结果，MVP 阶段只返回向量 ID，不保存真实向量值。
 */
@Data
@Builder
public class EmbeddingResult {

    /** 向量化模型名称。 */
    private String embeddingModel;

    /** 向量库中的向量 ID。 */
    private String vectorId;

    /** Query/document vector. MVP mock retrieval uses it; real providers can fill it when available. */
    private List<Double> vector;
}
