package com.example.aikb.service.retrieval;

import com.example.aikb.service.retrieval.adapter.RetrievalQueryEmbedding;

/**
 * 查询向量生成服务，隔离“用户问题向量化”和“文档 chunk 向量化”的调用语义。
 */
public interface QueryEmbeddingService {

    /**
     * 为用户问题生成查询向量。
     *
     * @param question 用户问题
     * @return 查询向量信息
     */
    RetrievalQueryEmbedding embed(String question);
}
