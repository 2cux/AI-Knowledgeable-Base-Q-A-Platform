package com.example.aikb.service.embedding;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 单个 chunk 的向量化结果，包含向量 ID 和可落库的向量内容。
 */
@Data
@Builder
public class EmbeddingResult {

    /** 向量化模型名称。 */
    private String embeddingModel;

    /** 向量库中的向量 ID。 */
    private String vectorId;

    /** 查询或文档向量。 */
    private List<Double> vector;
}
