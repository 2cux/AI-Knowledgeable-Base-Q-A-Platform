package com.example.aikb.service.retrieval.adapter.impl;

import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.ChunkEmbeddingMapper;
import com.example.aikb.service.embedding.mock.MockEmbeddingVectorizer;
import com.example.aikb.service.retrieval.adapter.RetrievalCandidate;
import com.example.aikb.service.retrieval.adapter.RetrievalQueryEmbedding;
import com.example.aikb.service.retrieval.adapter.VectorSearchAdapter;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * MVP 阶段的向量检索适配器。
 *
 * <p>该实现从 MySQL 读取已向量化成功的 chunk，根据 chunk 内容重新生成确定性的 mock 向量，
 * 再用余弦相似度对候选结果排序。生产环境应将检索下推到真实向量库中执行。</p>
 */
@Component
@RequiredArgsConstructor
public class MockVectorSearchAdapter implements VectorSearchAdapter {

    private static final String STATUS_SUCCESS = "SUCCESS";

    private final ChunkEmbeddingMapper chunkEmbeddingMapper;

    @Override
    public List<RetrievalCandidate> search(Long knowledgeBaseId, RetrievalQueryEmbedding queryEmbedding, int topK) {
        if (queryEmbedding == null || queryEmbedding.getVector() == null || queryEmbedding.getVector().isEmpty()) {
            throw new BusinessException("query embedding生成失败，无法检索");
        }

        List<RetrievalCandidate> candidates =
                chunkEmbeddingMapper.selectRetrievalCandidates(knowledgeBaseId, STATUS_SUCCESS);

        return candidates.stream()
                .peek(candidate -> candidate.setScore(cosine(
                        queryEmbedding.getVector(),
                        MockEmbeddingVectorizer.vectorize(candidate.getContent()))))
                .filter(candidate -> candidate.getScore() > 0D)
                .sorted(Comparator.comparing(RetrievalCandidate::getScore).reversed()
                        .thenComparing(RetrievalCandidate::getChunkId))
                .limit(topK)
                .toList();
    }

    private double cosine(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0D;
        }

        int size = Math.min(left.size(), right.size());
        double dot = 0D;
        double leftNorm = 0D;
        double rightNorm = 0D;
        for (int i = 0; i < size; i++) {
            double leftValue = left.get(i);
            double rightValue = right.get(i);
            dot += leftValue * rightValue;
            leftNorm += leftValue * leftValue;
            rightNorm += rightValue * rightValue;
        }

        if (leftNorm == 0D || rightNorm == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
