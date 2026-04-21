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

/**
 * 问答服务实现，串联知识库权限校验、会话上下文、检索、答案生成和问答记录保存。
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int HISTORY_LIMIT = 5;
    private static final double MIN_RELEVANCE_SCORE = 0.05D;

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ChatRecordMapper chatRecordMapper;
    private final RetrievalService retrievalService;
    private final AnswerGeneratorService answerGeneratorService;
    private final ChatRecordService chatRecordService;
    private final ObjectMapper objectMapper;

    @Override
    public ChatAskResponse ask(ChatAskRequest request) {
        Long userId = CurrentUser.getUserId();
        KnowledgeBase knowledgeBase = getOwnKnowledgeBase(request.getKnowledgeBaseId(), userId);
        String question = request.getQuestion().trim();
        int topK = request.getTopK() == null ? DEFAULT_TOP_K : request.getTopK();
        boolean hasConversationId = request.getConversationId() != null;
        String conversationId = resolveConversationId(request.getConversationId());
        List<ChatRecord> historyRecords = loadHistoryRecords(userId, knowledgeBase.getId(), conversationId,
                hasConversationId);

        RetrievalSearchRequest retrievalRequest = new RetrievalSearchRequest();
        retrievalRequest.setKnowledgeBaseId(knowledgeBase.getId());
        retrievalRequest.setQuery(question);
        retrievalRequest.setTopK(topK);

        RetrievalSearchVO retrievalResult = retrievalService.search(retrievalRequest);
        List<RetrievalChunkVO> chunks = retrievalResult == null || retrievalResult.getChunks() == null
                ? Collections.emptyList()
                : retrievalResult.getChunks();

        List<RetrievalChunkVO> effectiveChunks = filterRelevantChunks(chunks);
        boolean matched = !effectiveChunks.isEmpty();
        List<CitationVO> citations = matched
                ? effectiveChunks.stream().map(this::toCitation).toList()
                : Collections.emptyList();
        String answer = answerGeneratorService.generate(question, effectiveChunks, historyRecords);

        saveRecord(userId, knowledgeBase.getId(), conversationId, question, answer, matched, effectiveChunks.size(), topK,
                citations);

        return ChatAskResponse.builder()
                .conversationId(conversationId)
                .answer(answer)
                .matched(matched)
                .retrievedChunkCount(effectiveChunks.size())
                .citations(citations)
                .build();
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null) {
            return UUID.randomUUID().toString();
        }
        String trimmed = conversationId.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(40001, "conversationId不能为空白");
        }
        return trimmed;
    }

    private List<ChatRecord> loadHistoryRecords(Long userId, Long knowledgeBaseId, String conversationId,
            boolean requireExistingConversation) {
        List<ChatRecord> records = chatRecordMapper.selectList(new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getUserId, userId)
                .eq(ChatRecord::getKnowledgeBaseId, knowledgeBaseId)
                .eq(ChatRecord::getConversationId, conversationId)
                .orderByDesc(ChatRecord::getCreatedAt)
                .orderByDesc(ChatRecord::getId)
                .last("LIMIT " + HISTORY_LIMIT));
        if (requireExistingConversation && records.isEmpty()) {
            throw new BusinessException(40400, "会话不存在");
        }
        List<ChatRecord> orderedRecords = new ArrayList<>(records);
        Collections.reverse(orderedRecords);
        return orderedRecords;
    }

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

    private List<RetrievalChunkVO> filterRelevantChunks(List<RetrievalChunkVO> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }
        return chunks.stream()
                .filter(chunk -> chunk.getScore() != null && chunk.getScore() >= MIN_RELEVANCE_SCORE)
                .toList();
    }

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
