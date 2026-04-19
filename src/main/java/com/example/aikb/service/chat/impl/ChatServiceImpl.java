package com.example.aikb.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aikb.dto.chat.ChatAskRequest;
import com.example.aikb.dto.retrieval.RetrievalSearchRequest;
import com.example.aikb.entity.ChatRecord;
import com.example.aikb.entity.KnowledgeBase;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.ChatRecordMapper;
import com.example.aikb.mapper.KnowledgeBaseMapper;
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.chat.AnswerGeneratorService;
import com.example.aikb.service.chat.ChatRecordService;
import com.example.aikb.service.chat.ChatService;
import com.example.aikb.service.retrieval.RetrievalService;
import com.example.aikb.vo.chat.ChatAskResponse;
import com.example.aikb.vo.chat.CitationVO;
import com.example.aikb.vo.retrieval.RetrievalChunkVO;
import com.example.aikb.vo.retrieval.RetrievalSearchVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 问答服务实现，串联知识库权限校验、检索、答案生成和问答记录保存。
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int DEFAULT_TOP_K = 5;
    private static final String NO_MATCH_ANSWER = "未在知识库中检索到相关内容，请补充或向量化相关文档后再试。";

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ChatRecordMapper chatRecordMapper;
    private final RetrievalService retrievalService;
    private final AnswerGeneratorService answerGeneratorService;
    private final ChatRecordService chatRecordService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatAskResponse ask(ChatAskRequest request) {
        Long userId = CurrentUser.getUserId();
        KnowledgeBase knowledgeBase = getOwnKnowledgeBase(request.getKnowledgeBaseId(), userId);
        String question = request.getQuestion().trim();
        int topK = request.getTopK() == null ? DEFAULT_TOP_K : request.getTopK();
        boolean hasConversationId = request.getConversationId() != null && !request.getConversationId().trim().isEmpty();
        String conversationId = resolveConversationId(request.getConversationId());
        List<ChatRecord> historyRecords = loadHistoryRecords(userId, knowledgeBase.getId(), conversationId,
                hasConversationId);

        RetrievalSearchRequest retrievalRequest = new RetrievalSearchRequest();
        retrievalRequest.setKnowledgeBaseId(knowledgeBase.getId());
        retrievalRequest.setQuestion(question);
        retrievalRequest.setTopK(topK);

        RetrievalSearchVO retrievalResult = retrievalService.search(retrievalRequest);
        List<RetrievalChunkVO> chunks = retrievalResult == null || retrievalResult.getChunks() == null
                ? Collections.emptyList()
                : retrievalResult.getChunks();

        boolean matched = !chunks.isEmpty();
        List<CitationVO> citations = matched ? chunks.stream().map(this::toCitation).toList() : Collections.emptyList();
        String answer = answerGeneratorService.generate(question, chunks, historyRecords);

        saveRecord(userId, knowledgeBase.getId(), conversationId, question, answer, matched, chunks.size(), topK, citations);

        return ChatAskResponse.builder()
                .conversationId(conversationId)
                .answer(answer)
                .matched(matched)
                .retrievedChunkCount(chunks.size())
                .citations(citations)
                .build();
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }
        return conversationId.trim();
    }

    private List<ChatRecord> loadHistoryRecords(Long userId, Long knowledgeBaseId, String conversationId,
            boolean requireExistingConversation) {
        List<ChatRecord> records = chatRecordMapper.selectList(new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getUserId, userId)
                .eq(ChatRecord::getKnowledgeBaseId, knowledgeBaseId)
                .eq(ChatRecord::getConversationId, conversationId)
                .orderByDesc(ChatRecord::getCreatedAt)
                .orderByDesc(ChatRecord::getId)
                .last("LIMIT 5"));
        if (requireExistingConversation && records.isEmpty()) {
            throw new BusinessException(40400, "会话不存在");
        }
        List<ChatRecord> orderedRecords = new ArrayList<>(records);
        Collections.reverse(orderedRecords);
        return orderedRecords;
    }

    /**
     * 校验指定知识库是否属于当前登录用户。
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

    /**
     * 将检索命中的切片转换为返回给前端的引用来源。
     */
    private CitationVO toCitation(RetrievalChunkVO chunk) {
        return CitationVO.builder()
                .chunkId(chunk.getChunkId())
                .documentId(chunk.getDocumentId())
                .knowledgeBaseId(chunk.getKnowledgeBaseId())
                .chunkIndex(chunk.getChunkIndex())
                .documentName(chunk.getDocumentName())
                .score(chunk.getScore())
                .contentSnippet(shorten(chunk.getContent(), 300))
                .build();
    }

    /**
     * 保存问答日志，包含命中状态和引用来源，便于后续审计和问题追踪。
     */
    private void saveRecord(Long userId, Long knowledgeBaseId, String conversationId, String question, String answer,
            boolean matched, int retrievedChunkCount, int topK, List<CitationVO> citations) {
        ChatRecord record = new ChatRecord();
        record.setUserId(userId);
        record.setKnowledgeBaseId(knowledgeBaseId);
        record.setConversationId(conversationId);
        record.setQuestion(question);
        record.setAnswer(answer);
        record.setMatched(matched);
        record.setRetrievedChunkCount(retrievedChunkCount);
        record.setTopK(topK);
        record.setCitationsJson(toJson(citations));
        record.setCreatedAt(LocalDateTime.now());
        chatRecordService.save(record);
    }

    private String toJson(List<CitationVO> citations) {
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(50001, "引用来源序列化失败");
        }
    }

    private String shorten(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
