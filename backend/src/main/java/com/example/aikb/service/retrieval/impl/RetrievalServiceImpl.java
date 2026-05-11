package com.example.aikb.service.retrieval.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aikb.common.LogSanitizer;
import com.example.aikb.config.AppRagRetrievalProperties;
import com.example.aikb.dto.retrieval.RetrievalSearchRequest;
import com.example.aikb.entity.KnowledgeBase;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.KnowledgeBaseMapper;
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.retrieval.QueryEmbeddingService;
import com.example.aikb.service.retrieval.RetrievalService;
import com.example.aikb.service.retrieval.adapter.RetrievalCandidate;
import com.example.aikb.service.retrieval.adapter.RetrievalQueryEmbedding;
import com.example.aikb.service.retrieval.adapter.VectorSearchAdapter;
import com.example.aikb.vo.retrieval.RetrievalChunkVO;
import com.example.aikb.vo.retrieval.RetrievalSearchVO;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 检索服务实现，负责权限校验、生成查询向量，并将相关性排序委托给向量检索适配层。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RetrievalServiceImpl implements RetrievalService {

    private static final int MIN_TOP_K = 1;
    private static final int MAX_TOP_K = 20;
    private static final int KNOWLEDGE_BASE_ACTIVE_STATUS = 1;
    private static final int KNOWLEDGE_BASE_NOT_DELETED = 0;

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final QueryEmbeddingService queryEmbeddingService;
    private final VectorSearchAdapter vectorSearchAdapter;
    private final AppRagRetrievalProperties retrievalProperties;

    @Override
    public RetrievalSearchVO search(RetrievalSearchRequest request) {
        Long userId = CurrentUser.getUserId();
        KnowledgeBase knowledgeBase = getOwnKnowledgeBase(request.getKnowledgeBaseId(), userId);
        int topK = resolveTopK(request.getTopK());
        String query = resolveQuery(request);
        long start = System.currentTimeMillis();

        RetrievalQueryEmbedding queryEmbedding = queryEmbeddingService.embed(query);

        List<RetrievalChunkVO> rawChunks = vectorSearchAdapter.search(knowledgeBase.getId(), queryEmbedding, topK)
                .stream()
                .map(this::toVO)
                .toList();
        double minEffectiveScore = retrievalProperties.getMinEffectiveScore();
        List<RetrievalChunkVO> effectiveChunks = filterEffectiveChunks(rawChunks, minEffectiveScore);

        log.info("Retrieval finished, userId={}, knowledgeBaseId={}, queryLength={}, queryPreview={}, topK={}, minEffectiveScore={}, rawRetrievedChunkCount={}, effectiveChunkCount={}, matched={}, highestScore={}, durationMs={}",
                userId,
                knowledgeBase.getId(),
                query.length(),
                LogSanitizer.preview(query, 80),
                topK,
                minEffectiveScore,
                rawChunks.size(),
                effectiveChunks.size(),
                !effectiveChunks.isEmpty(),
                rawChunks.stream()
                        .map(RetrievalChunkVO::getScore)
                        .filter(score -> score != null && Double.isFinite(score))
                        .findFirst()
                        .orElse(null),
                System.currentTimeMillis() - start);

        return RetrievalSearchVO.builder()
                .knowledgeBaseId(knowledgeBase.getId())
                .question(query)
                .topK(topK)
                .total(rawChunks.size())
                .chunks(rawChunks)
                .rawChunks(rawChunks)
                .effectiveChunks(effectiveChunks)
                .rawRetrievedChunkCount(rawChunks.size())
                .effectiveChunkCount(effectiveChunks.size())
                .minEffectiveScore(minEffectiveScore)
                .build();
    }

    /**
     * 在检索前校验当前用户是否可以访问指定知识库。
     */
    private KnowledgeBase getOwnKnowledgeBase(Long knowledgeBaseId, Long userId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, knowledgeBaseId)
                .eq(KnowledgeBase::getOwnerId, userId)
                .eq(KnowledgeBase::getStatus, KNOWLEDGE_BASE_ACTIVE_STATUS)
                .eq(KnowledgeBase::getDeleted, KNOWLEDGE_BASE_NOT_DELETED)
                .last("LIMIT 1"));
        if (knowledgeBase == null) {
            throw new BusinessException(40400, "知识库不存在");
        }
        return knowledgeBase;
    }

    private int resolveTopK(Integer topK) {
        int resolvedTopK = topK == null ? retrievalProperties.getTopK() : topK;
        if (resolvedTopK < MIN_TOP_K) {
            throw new BusinessException("topK 必须大于 0");
        }
        if (resolvedTopK > MAX_TOP_K) {
            throw new BusinessException("topK 不能超过 20");
        }
        return resolvedTopK;
    }

    private String resolveQuery(RetrievalSearchRequest request) {
        if (StringUtils.hasText(request.getQuery())) {
            return request.getQuery().trim();
        }
        if (StringUtils.hasText(request.getQuestion())) {
            return request.getQuestion().trim();
        }
        throw new BusinessException("query不能为空");
    }

    private RetrievalChunkVO toVO(RetrievalCandidate candidate) {
        return RetrievalChunkVO.builder()
                .chunkId(candidate.getChunkId())
                .documentId(candidate.getDocumentId())
                .knowledgeBaseId(candidate.getKnowledgeBaseId())
                .chunkIndex(candidate.getChunkIndex())
                .content(candidate.getContent())
                .score(candidate.getScore())
                .documentName(candidate.getDocumentName())
                .build();
    }

    private List<RetrievalChunkVO> filterEffectiveChunks(List<RetrievalChunkVO> rawChunks, double minEffectiveScore) {
        if (rawChunks == null || rawChunks.isEmpty()) {
            return Collections.emptyList();
        }
        return rawChunks.stream()
                .filter(chunk -> {
                    Double score = chunk.getScore();
                    return score != null && Double.isFinite(score) && score >= minEffectiveScore;
                })
                .toList();
    }
}
