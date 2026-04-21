package com.example.aikb.service.retrieval.impl;

import com.example.aikb.service.embedding.EmbeddingClient;
import com.example.aikb.service.embedding.EmbeddingResult;
import com.example.aikb.service.retrieval.QueryEmbeddingService;
import com.example.aikb.service.retrieval.adapter.RetrievalQueryEmbedding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * MVP 查询向量生成实现。当前复用 EmbeddingClient，后续可替换为真实 query embedding 调用。
 */
@Service
@RequiredArgsConstructor
public class QueryEmbeddingServiceImpl implements QueryEmbeddingService {

    private static final String DEFAULT_EMBEDDING_MODEL = "local-hash-embedding-v1";

    private final EmbeddingClient embeddingClient;

    @Override
    public RetrievalQueryEmbedding embed(String question) {
        EmbeddingResult result = embeddingClient.embed(null, question, DEFAULT_EMBEDDING_MODEL);
        return RetrievalQueryEmbedding.builder()
                .embeddingModel(result.getEmbeddingModel())
                .vectorId(result.getVectorId())
                .vector(result.getVector())
                .build();
    }
}
