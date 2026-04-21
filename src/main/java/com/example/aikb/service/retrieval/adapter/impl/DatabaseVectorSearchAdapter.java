package com.example.aikb.service.retrieval.adapter.impl;

import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.ChunkEmbeddingMapper;
import com.example.aikb.service.retrieval.adapter.RetrievalCandidate;
import com.example.aikb.service.retrieval.adapter.RetrievalQueryEmbedding;
import com.example.aikb.service.retrieval.adapter.VectorSearchAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MVP 阶段的真实检索适配器。
 *
 * <p>从 chunk_embedding.vector_json 读取已落库向量，并在应用层计算候选 chunk 的相似度。
 * 后续接入专用向量数据库时，可以替换该适配器而不影响 Controller 和 Service 契约。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseVectorSearchAdapter implements VectorSearchAdapter {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final int CANDIDATE_LIMIT = 2000;

    private final ChunkEmbeddingMapper chunkEmbeddingMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<RetrievalCandidate> search(Long knowledgeBaseId, RetrievalQueryEmbedding queryEmbedding, int topK) {
        if (queryEmbedding == null || queryEmbedding.getVector() == null || queryEmbedding.getVector().isEmpty()) {
            throw new BusinessException("query embedding生成失败，无法检索");
        }

        List<RetrievalCandidate> candidates =
                chunkEmbeddingMapper.selectRetrievalCandidates(
                        knowledgeBaseId, STATUS_SUCCESS, queryEmbedding.getEmbeddingModel(), CANDIDATE_LIMIT);

        return candidates.stream()
                .map(candidate -> scoreCandidate(candidate, queryEmbedding.getVector()))
                .filter(candidate -> candidate.getScore() > 0D)
                .sorted(Comparator.comparing(RetrievalCandidate::getScore).reversed()
                        .thenComparing(RetrievalCandidate::getChunkId))
                .limit(topK)
                .toList();
    }

    private RetrievalCandidate scoreCandidate(RetrievalCandidate candidate, List<Double> queryVector) {
        List<Double> chunkVector = parseVector(candidate);
        candidate.setScore(cosine(queryVector, chunkVector));
        return candidate;
    }

    private List<Double> parseVector(RetrievalCandidate candidate) {
        try {
            return objectMapper.readValue(candidate.getVectorJson(), new TypeReference<List<Double>>() {});
        } catch (JsonProcessingException ex) {
            log.warn("Skip invalid chunk embedding vector, chunkId={}, vectorId={}",
                    candidate.getChunkId(), candidate.getVectorId());
            return List.of();
        }
    }

    private double cosine(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0D;
        }

        if (left.size() != right.size()) {
            return 0D;
        }

        int size = left.size();
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
