package com.example.aikb.service.retrieval.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aikb.dto.retrieval.RetrievalSearchRequest;
import com.example.aikb.entity.KnowledgeBase;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.KnowledgeBaseMapper;
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.embedding.EmbeddingClient;
import com.example.aikb.service.embedding.EmbeddingResult;
import com.example.aikb.service.retrieval.RetrievalService;
import com.example.aikb.service.retrieval.adapter.RetrievalCandidate;
import com.example.aikb.service.retrieval.adapter.RetrievalQueryEmbedding;
import com.example.aikb.service.retrieval.adapter.VectorSearchAdapter;
import com.example.aikb.vo.retrieval.RetrievalChunkVO;
import com.example.aikb.vo.retrieval.RetrievalSearchVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 检索服务实现。负责权限校验、生成查询向量，并将相关性排序委托给向量检索适配层。
 */
@Service
@RequiredArgsConstructor
public class RetrievalServiceImpl implements RetrievalService {

    private static final String DEFAULT_EMBEDDING_MODEL = "mock-embedding-v1";
    private static final int DEFAULT_TOP_K = 5;

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final EmbeddingClient embeddingClient;
    private final VectorSearchAdapter vectorSearchAdapter;

    @Override
    public RetrievalSearchVO search(RetrievalSearchRequest request) {
        Long userId = CurrentUser.getUserId();
        KnowledgeBase knowledgeBase = getOwnKnowledgeBase(request.getKnowledgeBaseId(), userId);
        int topK = request.getTopK() == null ? DEFAULT_TOP_K : request.getTopK();
        String question = request.getQuestion().trim();

        EmbeddingResult embeddingResult = embeddingClient.embed(0L, question, DEFAULT_EMBEDDING_MODEL);
        RetrievalQueryEmbedding queryEmbedding = RetrievalQueryEmbedding.builder()
                .embeddingModel(embeddingResult.getEmbeddingModel())
                .vectorId(embeddingResult.getVectorId())
                .vector(embeddingResult.getVector())
                .build();

        List<RetrievalChunkVO> chunks = vectorSearchAdapter.search(knowledgeBase.getId(), queryEmbedding, topK)
                .stream()
                .map(this::toVO)
                .toList();

        return RetrievalSearchVO.builder()
                .knowledgeBaseId(knowledgeBase.getId())
                .question(question)
                .topK(topK)
                .total(chunks.size())
                .chunks(chunks)
                .build();
    }

    /**
     * 在检索前校验当前用户是否可以访问指定知识库。
     */
    private KnowledgeBase getOwnKnowledgeBase(Long knowledgeBaseId, Long userId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, knowledgeBaseId)
                .eq(KnowledgeBase::getOwnerId, userId)
                .eq(KnowledgeBase::getStatus, 1)
                .last("LIMIT 1"));
        if (knowledgeBase == null) {
            throw new BusinessException(40400, "知识库不存在");
        }
        return knowledgeBase;
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
}
