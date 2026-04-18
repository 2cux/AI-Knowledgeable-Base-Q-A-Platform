package com.example.aikb.service.retrieval.adapter;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 传递给向量检索适配层的查询向量信息。
 */
@Data
@Builder
public class RetrievalQueryEmbedding {

    /** 查询向量使用的 embedding 模型。 */
    private String embeddingModel;

    /** embedding 服务返回的向量 ID，后续接入真实向量库时可用于追踪。 */
    private String vectorId;

    /** 查询向量。MVP mock 适配器使用该字段计算余弦相似度。 */
    private List<Double> vector;
}
