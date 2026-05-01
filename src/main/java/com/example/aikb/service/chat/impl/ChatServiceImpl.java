package com.example.aikb.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aikb.config.AppRagRetrievalProperties;
import com.example.aikb.dto.chat.ChatAskRequest;
import com.example.aikb.dto.retrieval.RetrievalSearchRequest;
import com.example.aikb.entity.ChatRecord;
import com.example.aikb.entity.KnowledgeBase;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.ChatRecordMapper;
import com.example.aikb.mapper.KnowledgeBaseMapper;
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.chat.AnswerGenerationResult;
import com.example.aikb.service.chat.AnswerGeneratorService;
import com.example.aikb.service.chat.AnswerStatus;
import com.example.aikb.service.chat.ChatRecordService;
import com.example.aikb.service.chat.ChatService;
import com.example.aikb.service.retrieval.RetrievalService;
import com.example.aikb.vo.chat.ChatAskResponse;
import com.example.aikb.vo.chat.CitationVO;
import com.example.aikb.vo.retrieval.RetrievalChunkVO;
import com.example.aikb.vo.retrieval.RetrievalSearchVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Chat ask service for retrieval, answer generation and chat record persistence.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int HISTORY_LIMIT = 5;
    private static final String NO_HIT_ANSWER =
            "\u672a\u68c0\u7d22\u5230\u76f8\u5173\u77e5\u8bc6\u7247\u6bb5\uff0c"
                    + "\u65e0\u6cd5\u57fa\u4e8e\u5f53\u524d\u77e5\u8bc6\u5e93"
                    + "\u56de\u7b54\u8be5\u95ee\u9898\u3002";
    private static final String WEAK_HIT_ANSWER =
            "\u68c0\u7d22\u5230\u7684\u77e5\u8bc6\u7247\u6bb5\u76f8\u5173\u6027\u8f83\u5f31\uff0c"
                    + "\u65e0\u6cd5\u652f\u6301\u53ef\u9760\u56de\u7b54\u3002";
    private static final String RETRIEVAL_UNAVAILABLE_ANSWER =
            "\u77e5\u8bc6\u5e93\u68c0\u7d22\u670d\u52a1\u6682\u65f6\u4e0d\u53ef\u7528\uff0c"
                    + "\u65e0\u6cd5\u57fa\u4e8e\u5f53\u524d\u77e5\u8bc6\u5e93"
                    + "\u751f\u6210\u53ef\u9760\u56de\u7b54\u3002\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002";

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final ChatRecordMapper chatRecordMapper;
    private final RetrievalService retrievalService;
    private final AnswerGeneratorService answerGeneratorService;
    private final ChatRecordService chatRecordService;
    private final CitationJsonCodec citationJsonCodec;
    private final AppRagRetrievalProperties retrievalProperties;

    @Override
    public ChatAskResponse ask(ChatAskRequest request) {
        Long userId = CurrentUser.getUserId();
        KnowledgeBase knowledgeBase = getOwnKnowledgeBase(request.getKnowledgeBaseId(), userId);
        String question = request.getQuestion().trim();
        int topK = request.getTopK() == null ? retrievalProperties.getTopK() : request.getTopK();
        boolean hasConversationId = request.getConversationId() != null;
        String conversationId = resolveConversationId(request.getConversationId());
        List<ChatRecord> historyRecords = loadHistoryRecords(userId, knowledgeBase.getId(), conversationId,
                hasConversationId);

        RetrievalSearchRequest retrievalRequest = new RetrievalSearchRequest();
        retrievalRequest.setKnowledgeBaseId(knowledgeBase.getId());
        retrievalRequest.setQuery(question);
        retrievalRequest.setTopK(topK);

        RetrievalSearchVO retrievalResult;
        try {
            retrievalResult = retrievalService.search(retrievalRequest);
        } catch (BusinessException ex) {
            log.warn("Chat RAG retrieval unavailable, userId={}, knowledgeBaseId={}, conversationId={}, questionLength={}, topK={}, error={}",
                    userId, knowledgeBase.getId(), conversationId, question.length(), topK, ex.getMessage());
            return retrievalUnavailable(userId, knowledgeBase.getId(), conversationId, question, topK);
        } catch (RuntimeException ex) {
            log.warn("Chat RAG retrieval unavailable unexpectedly, userId={}, knowledgeBaseId={}, conversationId={}, questionLength={}, topK={}, errorType={}",
                    userId, knowledgeBase.getId(), conversationId, question.length(), topK,
                    ex.getClass().getSimpleName());
            return retrievalUnavailable(userId, knowledgeBase.getId(), conversationId, question, topK);
        }
        List<RetrievalChunkVO> rawChunks = retrievalResult == null || retrievalResult.getRawChunks() == null
                ? Collections.emptyList()
                : retrievalResult.getRawChunks();
        List<RetrievalChunkVO> effectiveChunks = retrievalResult == null || retrievalResult.getEffectiveChunks() == null
                ? Collections.emptyList()
                : retrievalResult.getEffectiveChunks();

        AnswerResolution resolution = resolveAnswer(question, historyRecords, rawChunks, effectiveChunks);
        Double minEffectiveScore = retrievalResult == null ? null : retrievalResult.getMinEffectiveScore();

        saveRecord(userId, knowledgeBase.getId(), conversationId, question, resolution.answer(),
                resolution.answerStatus(), resolution.matched(), effectiveChunks.size(), rawChunks.size(), topK,
                resolution.citations());

        log.info("Chat RAG retrieval resolved, userId={}, knowledgeBaseId={}, conversationId={}, questionLength={}, topK={}, minEffectiveScore={}, rawRetrievedChunkCount={}, effectiveChunkCount={}, matched={}, answerStatus={}, llmCalled={}",
                userId, knowledgeBase.getId(), conversationId, question.length(), topK,
                retrievalResult == null ? null : retrievalResult.getMinEffectiveScore(),
                rawChunks.size(), effectiveChunks.size(), resolution.matched(), resolution.answerStatus(),
                resolution.llmCalled());

        return ChatAskResponse.builder()
                .conversationId(conversationId)
                .answer(resolution.answer())
                .answerStatus(resolution.answerStatus())
                .matched(resolution.matched())
                .retrievedChunkCount(effectiveChunks.size())
                .rawRetrievedChunkCount(rawChunks.size())
                .effectiveChunkCount(effectiveChunks.size())
                .minEffectiveScore(minEffectiveScore)
                .citations(resolution.citations())
                .build();
    }

    private AnswerResolution resolveAnswer(String question, List<ChatRecord> historyRecords,
            List<RetrievalChunkVO> rawChunks, List<RetrievalChunkVO> effectiveChunks) {
        if (rawChunks.isEmpty()) {
            return new AnswerResolution(NO_HIT_ANSWER, AnswerStatus.NO_HIT, false, Collections.emptyList(), false);
        }
        if (effectiveChunks.isEmpty()) {
            return new AnswerResolution(WEAK_HIT_ANSWER, AnswerStatus.WEAK_HIT, false, Collections.emptyList(), false);
        }

        List<CitationVO> citations = effectiveChunks.stream().map(this::toCitation).toList();
        AnswerGenerationResult answerResult = answerGeneratorService.generate(question, effectiveChunks, historyRecords);
        AnswerStatus answerStatus = answerResult.isLlmAvailable()
                ? AnswerStatus.SUCCESS
                : AnswerStatus.LLM_UNAVAILABLE;
        return new AnswerResolution(answerResult.getAnswer(), answerStatus, true, citations, true);
    }

    private ChatAskResponse retrievalUnavailable(Long userId, Long knowledgeBaseId, String conversationId,
            String question, int topK) {
        saveRecord(userId, knowledgeBaseId, conversationId, question, RETRIEVAL_UNAVAILABLE_ANSWER,
                AnswerStatus.RETRIEVAL_UNAVAILABLE, false, 0, 0, topK, Collections.emptyList());
        log.info("Chat RAG retrieval resolved, userId={}, knowledgeBaseId={}, conversationId={}, questionLength={}, topK={}, minEffectiveScore={}, rawRetrievedChunkCount={}, effectiveChunkCount={}, matched={}, answerStatus={}, llmCalled={}",
                userId, knowledgeBaseId, conversationId, question.length(), topK, null, 0, 0, false,
                AnswerStatus.RETRIEVAL_UNAVAILABLE, false);
        return ChatAskResponse.builder()
                .conversationId(conversationId)
                .answer(RETRIEVAL_UNAVAILABLE_ANSWER)
                .answerStatus(AnswerStatus.RETRIEVAL_UNAVAILABLE)
                .matched(false)
                .retrievedChunkCount(0)
                .rawRetrievedChunkCount(0)
                .effectiveChunkCount(0)
                .minEffectiveScore(null)
                .citations(Collections.emptyList())
                .build();
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null) {
            return UUID.randomUUID().toString();
        }
        String trimmed = conversationId.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(40001, "conversationId\u4e0d\u80fd\u4e3a\u7a7a\u767d");
        }
        return trimmed;
    }

    private List<ChatRecord> loadHistoryRecords(Long userId, Long knowledgeBaseId, String conversationId,
            boolean requireExistingConversation) {
        List<ChatRecord> records = chatRecordMapper.selectList(new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getUserId, userId)
                .eq(ChatRecord::getKnowledgeBaseId, knowledgeBaseId)
                .eq(ChatRecord::getConversationId, conversationId)
                .eq(ChatRecord::getAnswerStatus, AnswerStatus.SUCCESS)
                .orderByDesc(ChatRecord::getCreatedAt)
                .orderByDesc(ChatRecord::getId)
                .last("LIMIT " + HISTORY_LIMIT));
        if (requireExistingConversation && records.isEmpty()
                && !conversationExists(userId, knowledgeBaseId, conversationId)) {
            throw new BusinessException(40400, "\u4f1a\u8bdd\u4e0d\u5b58\u5728");
        }
        List<ChatRecord> orderedRecords = new ArrayList<>(records);
        Collections.reverse(orderedRecords);
        return orderedRecords;
    }

    private boolean conversationExists(Long userId, Long knowledgeBaseId, String conversationId) {
        Long count = chatRecordMapper.selectCount(new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getUserId, userId)
                .eq(ChatRecord::getKnowledgeBaseId, knowledgeBaseId)
                .eq(ChatRecord::getConversationId, conversationId));
        return count != null && count > 0;
    }

    private KnowledgeBase getOwnKnowledgeBase(Long knowledgeBaseId, Long userId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, knowledgeBaseId)
                .eq(KnowledgeBase::getOwnerId, userId)
                .eq(KnowledgeBase::getStatus, 1)
                .last("LIMIT 1"));
        if (knowledgeBase == null) {
            throw new BusinessException(40400, "\u77e5\u8bc6\u5e93\u4e0d\u5b58\u5728");
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

    private void saveRecord(Long userId, Long knowledgeBaseId, String conversationId, String question, String answer,
            AnswerStatus answerStatus, boolean matched, int retrievedChunkCount, int rawRetrievedChunkCount, int topK,
            List<CitationVO> citations) {
        ChatRecord record = new ChatRecord();
        record.setUserId(userId);
        record.setKnowledgeBaseId(knowledgeBaseId);
        record.setConversationId(conversationId);
        record.setQuestion(question);
        record.setAnswer(answer);
        record.setAnswerStatus(answerStatus);
        record.setMatched(matched);
        record.setRetrievedChunkCount(retrievedChunkCount);
        record.setRawRetrievedChunkCount(rawRetrievedChunkCount);
        record.setTopK(topK);
        record.setCitationsJson(citationJsonCodec.serialize(citations));
        record.setCreatedAt(LocalDateTime.now());
        chatRecordService.save(record);
    }

    private String shorten(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    private record AnswerResolution(String answer, AnswerStatus answerStatus, boolean matched,
            List<CitationVO> citations, boolean llmCalled) {
    }
}
