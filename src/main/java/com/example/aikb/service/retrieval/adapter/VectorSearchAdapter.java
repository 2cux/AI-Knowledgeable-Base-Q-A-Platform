package com.example.aikb.service.retrieval.adapter;

import java.util.List;

/**
 * 向量检索适配层。后续接入 Milvus、pgvector、Elasticsearch KNN 或其他向量库时，
 * 只需要替换实现类，不需要改动 controller 和 retrieval service 的调用契约。
 */
public interface VectorSearchAdapter {

    /**
     * 使用查询向量在指定知识库中检索相关 chunk。
     *
     * @param knowledgeBaseId 知识库范围
     * @param queryEmbedding 查询向量
     * @param topK 最大返回数量
     * @return 按相关性排序后的候选结果
     */
    List<RetrievalCandidate> search(Long knowledgeBaseId, RetrievalQueryEmbedding queryEmbedding, int topK);
}
