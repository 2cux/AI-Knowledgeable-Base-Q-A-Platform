package com.example.aikb.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aikb.config.AppRagRetrievalProperties;
import com.example.aikb.dto.chat.ChatAskRequest;
import com.example.aikb.dto.retrieval.RetrievalSearchRequest;
import com.example.aikb.entity.ChatRecord;
import com.example.aikb.entity.Conversation;
import com.example.aikb.entity.KnowledgeBase;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.KnowledgeBaseMapper;
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.chat.AnswerGenerationResult;
import com.example.aikb.service.chat.AnswerGeneratorService;
import com.example.aikb.service.chat.AnswerStatus;
import com.example.aikb.service.chat.ChatRecordService;
import com.example.aikb.service.chat.ChatService;
import com.example.aikb.service.chat.ConversationContextLoader;
import com.example.aikb.service.chat.ConversationService;
import com.example.aikb.service.chat.MessageService;
import com.example.aikb.service.retrieval.RetrievalService;
import com.example.aikb.vo.chat.ChatAskResponse;
import com.example.aikb.vo.chat.CitationVO;
import com.example.aikb.vo.retrieval.RetrievalChunkVO;
import com.example.aikb.vo.retrieval.RetrievalSearchVO;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Chat ask service for retrieval, answer generation and conversation persistence.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

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
    private static final int KNOWLEDGE_BASE_ACTIVE_STATUS = 1;
    private static final int KNOWLEDGE_BASE_NOT_DELETED = 0;

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final RetrievalService retrievalService;
    private final AnswerGeneratorService answerGeneratorService;
    private final ChatRecordService chatRecordService;
    private final CitationJsonCodec citationJsonCodec;
    private final AppRagRetrievalProperties retrievalProperties;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ConversationContextLoader conversationContextLoader;
    private final TransactionTemplate transactionTemplate;

    @Override
    public ChatAskResponse ask(ChatAskRequest request) {
        Long userId = CurrentUser.getUserId();
        KnowledgeBase knowledgeBase = getOwnKnowledgeBase(request.getKnowledgeBaseId(), userId);
        String question = request.getQuestion().trim();
        int topK = request.getTopK() == null ? retrievalProperties.getTopK() : request.getTopK();
        Conversation conversation = conversationService.resolveForAsk(userId, knowledgeBase.getId(),
                request.getConversationId(), question);
        String conversationId = conversation.getConversationUid();
        String conversationContext = conversationContextLoader.load(userId, knowledgeBase.getId(), conversationId);

        RetrievalSearchRequest retrievalRequest = new RetrievalSearchRequest();
        retrievalRequest.setKnowledgeBaseId(knowledgeBase.getId());
        retrievalRequest.setQuery(question);
        retrievalRequest.setTopK(topK);

        RetrievalSearchVO retrievalResult;
        try {
            retrievalResult = retrievalService.search(retrievalRequest);
        } catch (BusinessException ex) {
            if (isClientBusinessException(ex)) {
                throw ex;
            }
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

        AnswerResolution resolution = resolveAnswer(question, conversationContext, rawChunks, effectiveChunks);
        Double minEffectiveScore = retrievalResult == null ? null : retrievalResult.getMinEffectiveScore();
        ChatRecord record = persistAskResult(userId, knowledgeBase.getId(), conversationId, question,
                resolution.answer(), resolution.answerStatus(), resolution.matched(), effectiveChunks.size(),
                rawChunks.size(), topK, resolution.citations());

        log.info("Chat RAG retrieval resolved, userId={}, knowledgeBaseId={}, conversationId={}, chatRecordId={}, questionLength={}, topK={}, minEffectiveScore={}, rawRetrievedChunkCount={}, effectiveChunkCount={}, matched={}, answerStatus={}, llmCalled={}",
                userId, knowledgeBase.getId(), conversationId, record.getId(), question.length(), topK,
                minEffectiveScore, rawChunks.size(), effectiveChunks.size(), resolution.matched(),
                resolution.answerStatus(), resolution.llmCalled());

        return buildResponse(conversationId, record.getId(), resolution.answer(), resolution.answerStatus(),
                resolution.matched(), effectiveChunks.size(), rawChunks.size(), minEffectiveScore,
                resolution.citations());
    }

    private AnswerResolution resolveAnswer(String question, String conversationContext,
            List<RetrievalChunkVO> rawChunks, List<RetrievalChunkVO> effectiveChunks) {
        if (rawChunks.isEmpty()) {
            return new AnswerResolution(NO_HIT_ANSWER, AnswerStatus.NO_HIT, false, Collections.emptyList(), false);
        }
        if (effectiveChunks.isEmpty()) {
            return new AnswerResolution(WEAK_HIT_ANSWER, AnswerStatus.WEAK_HIT, false, Collections.emptyList(), false);
        }

        List<CitationVO> citations = effectiveChunks.stream().map(this::toCitation).toList();
        AnswerGenerationResult answerResult = answerGeneratorService.generate(question, effectiveChunks,
                conversationContext);
        AnswerStatus answerStatus = answerResult.isLlmAvailable()
                ? AnswerStatus.SUCCESS
                : AnswerStatus.LLM_UNAVAILABLE;
        return new AnswerResolution(answerResult.getAnswer(), answerStatus, true, citations, true);
    }

    private ChatAskResponse retrievalUnavailable(Long userId, Long knowledgeBaseId, String conversationId,
            String question, int topK) {
        ChatRecord record = persistAskResult(userId, knowledgeBaseId, conversationId, question,
                RETRIEVAL_UNAVAILABLE_ANSWER, AnswerStatus.RETRIEVAL_UNAVAILABLE, false, 0, 0, topK,
                Collections.emptyList());
        log.info("Chat RAG retrieval resolved, userId={}, knowledgeBaseId={}, conversationId={}, chatRecordId={}, questionLength={}, topK={}, minEffectiveScore={}, rawRetrievedChunkCount={}, effectiveChunkCount={}, matched={}, answerStatus={}, llmCalled={}",
                userId, knowledgeBaseId, conversationId, record.getId(), question.length(), topK, null, 0, 0, false,
                AnswerStatus.RETRIEVAL_UNAVAILABLE, false);
        return buildResponse(conversationId, record.getId(), RETRIEVAL_UNAVAILABLE_ANSWER,
                AnswerStatus.RETRIEVAL_UNAVAILABLE, false, 0, 0, null, Collections.emptyList());
    }

    private boolean isClientBusinessException(BusinessException ex) {
        return ex.getHttpStatus() >= 400 && ex.getHttpStatus() < 500;
    }

    private KnowledgeBase getOwnKnowledgeBase(Long knowledgeBaseId, Long userId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, knowledgeBaseId)
                .eq(KnowledgeBase::getOwnerId, userId)
                .eq(KnowledgeBase::getStatus, KNOWLEDGE_BASE_ACTIVE_STATUS)
                .eq(KnowledgeBase::getDeleted, KNOWLEDGE_BASE_NOT_DELETED)
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

    private ChatRecord persistAskResult(Long userId, Long knowledgeBaseId, String conversationId, String question,
            String answer, AnswerStatus answerStatus, boolean matched, int retrievedChunkCount,
            int rawRetrievedChunkCount, int topK, List<CitationVO> citations) {
        return transactionTemplate.execute(status -> {
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

            messageService.saveUserMessage(userId, knowledgeBaseId, conversationId, question);
            messageService.saveAssistantMessage(userId, knowledgeBaseId, conversationId, answer, citations,
                    record.getId());
            conversationService.touchAfterAsk(conversationId, question, answer, 2);
            return record;
        });
    }

    private ChatAskResponse buildResponse(String conversationId, Long chatRecordId, String answer,
            AnswerStatus answerStatus, boolean matched, int effectiveChunkCount, int rawChunkCount,
            Double minEffectiveScore, List<CitationVO> citations) {
        return ChatAskResponse.builder()
                .conversationId(conversationId)
                .chatRecordId(chatRecordId)
                .answer(answer)
                .answerStatus(answerStatus)
                .matched(matched)
                .retrievedChunkCount(effectiveChunkCount)
                .rawRetrievedChunkCount(rawChunkCount)
                .effectiveChunkCount(effectiveChunkCount)
                .minEffectiveScore(minEffectiveScore)
                .citations(citations)
                .build();
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
