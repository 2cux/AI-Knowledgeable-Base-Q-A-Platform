package com.example.aikb.service.chat.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.aikb.config.AppRagRetrievalProperties;
import com.example.aikb.dto.chat.ChatAskRequest;
import com.example.aikb.entity.ChatRecord;
import com.example.aikb.entity.KnowledgeBase;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.entity.Conversation;
import com.example.aikb.mapper.KnowledgeBaseMapper;
import com.example.aikb.security.LoginUser;
import com.example.aikb.service.chat.AnswerGenerationResult;
import com.example.aikb.service.chat.AnswerGeneratorService;
import com.example.aikb.service.chat.AnswerStatus;
import com.example.aikb.service.chat.ChatRecordService;
import com.example.aikb.service.chat.ConversationContextLoader;
import com.example.aikb.service.chat.ConversationService;
import com.example.aikb.service.chat.ConversationTitleGenerateService;
import com.example.aikb.service.chat.MessageService;
import com.example.aikb.service.retrieval.RetrievalService;
import com.example.aikb.vo.chat.ChatAskResponse;
import com.example.aikb.vo.retrieval.RetrievalChunkVO;
import com.example.aikb.vo.retrieval.RetrievalSearchVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final Long KNOWLEDGE_BASE_ID = 11L;

    @Mock
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Mock
    private RetrievalService retrievalService;

    @Mock
    private AnswerGeneratorService answerGeneratorService;

    @Mock
    private ChatRecordService chatRecordService;

    @Mock
    private ConversationService conversationService;

    @Mock
    private MessageService messageService;

    @Mock
    private ConversationContextLoader conversationContextLoader;

    @Mock
    private TransactionTemplate transactionTemplate;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        AppRagRetrievalProperties retrievalProperties = new AppRagRetrievalProperties();
        retrievalProperties.setTopK(5);
        chatService = new ChatServiceImpl(knowledgeBaseMapper, retrievalService,
                answerGeneratorService, chatRecordService, new CitationJsonCodec(new ObjectMapper()),
                retrievalProperties, conversationService, messageService, conversationContextLoader,
                transactionTemplate, mock(ConversationTitleGenerateService.class));

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser(USER_ID, "tester"), null, Collections.emptyList()));
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(KNOWLEDGE_BASE_ID);
        knowledgeBase.setOwnerId(USER_ID);
        knowledgeBase.setStatus(1);
        lenient().when(knowledgeBaseMapper.selectOne(any())).thenReturn(knowledgeBase);
        lenient().when(chatRecordService.save(any())).thenAnswer(invocation -> {
            ChatRecord record = invocation.getArgument(0);
            record.setId(99L);
            return record;
        });
        lenient().when(conversationService.resolveForAsk(any(), any(), any(), any())).thenReturn(conversation());
        lenient().when(conversationContextLoader.load(any(), any(), any())).thenReturn("");
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void askReturnsNoHitWithoutCallingLlmWhenRawChunksAreEmpty() {
        when(retrievalService.search(any())).thenReturn(retrievalResult(Collections.emptyList(), Collections.emptyList()));

        ChatAskResponse response = chatService.ask(request());

        assertThat(response.getAnswerStatus()).isEqualTo(AnswerStatus.NO_HIT);
        assertThat(response.getMatched()).isFalse();
        assertThat(response.getCitations()).isEmpty();
        verify(answerGeneratorService, never()).generate(any(), any(), any());
        assertSavedRecord(AnswerStatus.NO_HIT, false, 0, 0, "[]");
    }

    @Test
    void askReturnsWeakHitWithoutCallingLlmWhenOnlyRawChunksExist() {
        when(retrievalService.search(any())).thenReturn(retrievalResult(List.of(chunk(0.1D)), Collections.emptyList()));

        ChatAskResponse response = chatService.ask(request());

        assertThat(response.getAnswerStatus()).isEqualTo(AnswerStatus.WEAK_HIT);
        assertThat(response.getRawRetrievedChunkCount()).isEqualTo(1);
        assertThat(response.getEffectiveChunkCount()).isZero();
        assertThat(response.getMatched()).isFalse();
        assertThat(response.getCitations()).isEmpty();
        verify(answerGeneratorService, never()).generate(any(), any(), any());
        assertSavedRecord(AnswerStatus.WEAK_HIT, false, 0, 1, "[]");
    }

    @Test
    void askReturnsSuccessWhenEffectiveChunksAndLlmSucceeds() {
        RetrievalChunkVO chunk = chunk(0.9D);
        when(retrievalService.search(any())).thenReturn(retrievalResult(List.of(chunk), List.of(chunk)));
        when(answerGeneratorService.generate(any(), any(), any())).thenReturn(AnswerGenerationResult.builder()
                .answer("answer")
                .llmAvailable(true)
                .build());

        ChatAskResponse response = chatService.ask(request());

        assertThat(response.getAnswerStatus()).isEqualTo(AnswerStatus.SUCCESS);
        assertThat(response.getMatched()).isTrue();
        assertThat(response.getAnswer()).isEqualTo("answer");
        assertThat(response.getCitations()).hasSize(1);
        assertSavedRecord(AnswerStatus.SUCCESS, true, 1, 1, "\"chunkId\":101");
    }

    @Test
    void askReturnsLlmUnavailableWhenEffectiveChunksExistButLlmFails() {
        RetrievalChunkVO chunk = chunk(0.9D);
        when(retrievalService.search(any())).thenReturn(retrievalResult(List.of(chunk), List.of(chunk)));
        when(answerGeneratorService.generate(any(), any(), any())).thenReturn(AnswerGenerationResult.builder()
                .answer("llm unavailable")
                .llmAvailable(false)
                .build());

        ChatAskResponse response = chatService.ask(request());

        assertThat(response.getAnswerStatus()).isEqualTo(AnswerStatus.LLM_UNAVAILABLE);
        assertThat(response.getMatched()).isTrue();
        assertThat(response.getAnswer()).isEqualTo("llm unavailable");
        assertThat(response.getCitations()).hasSize(1);
        assertSavedRecord(AnswerStatus.LLM_UNAVAILABLE, true, 1, 1, "\"chunkId\":101");
    }

    @Test
    void askReturnsRetrievalUnavailableWithoutCallingLlmWhenRetrievalFails() {
        when(retrievalService.search(any())).thenThrow(new BusinessException(50000, "embedding unavailable"));

        ChatAskResponse response = chatService.ask(request());

        assertThat(response.getAnswerStatus()).isEqualTo(AnswerStatus.RETRIEVAL_UNAVAILABLE);
        assertThat(response.getMatched()).isFalse();
        assertThat(response.getRetrievedChunkCount()).isZero();
        assertThat(response.getCitations()).isEmpty();
        verify(answerGeneratorService, never()).generate(any(), any(), any());
        assertSavedRecord(AnswerStatus.RETRIEVAL_UNAVAILABLE, false, 0, 0, "[]");
    }

    @Test
    void askRethrowsClientBusinessExceptionWithoutSavingRecord() {
        when(retrievalService.search(any())).thenThrow(new BusinessException(40000, "topK 必须大于 0"));

        assertThatThrownBy(() -> chatService.ask(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("topK 必须大于 0");

        verify(chatRecordService, never()).save(any());
    }

    @Test
    void askLoadsConversationContextForExistingConversation() {
        ChatAskRequest request = request();
        request.setConversationId("conversation-1");
        RetrievalChunkVO chunk = chunk(0.9D);
        lenient().when(conversationService.resolveForAsk(any(), any(), any(), any())).thenReturn(conversation("conversation-1"));
        when(conversationContextLoader.load(any(), any(), any())).thenReturn("context text");
        when(retrievalService.search(any())).thenReturn(retrievalResult(List.of(chunk), List.of(chunk)));
        when(answerGeneratorService.generate(any(), any(), any())).thenReturn(AnswerGenerationResult.builder()
                .answer("answer")
                .llmAvailable(true)
                .build());

        chatService.ask(request);

        ArgumentCaptor<String> contextCaptor = ArgumentCaptor.forClass(String.class);
        verify(answerGeneratorService).generate(any(), any(), contextCaptor.capture());
        assertThat(contextCaptor.getValue()).isEqualTo("context text");
        verify(conversationContextLoader).load(USER_ID, KNOWLEDGE_BASE_ID, "conversation-1");
    }

    @Test
    void citationCodecReturnsEmptyListForBlankOrInvalidJson() {
        CitationJsonCodec codec = new CitationJsonCodec(new ObjectMapper());

        assertThat(codec.deserialize(null, 1L)).isEmpty();
        assertThat(codec.deserialize("", 1L)).isEmpty();
        assertThat(codec.deserialize("not-json", 1L)).isEmpty();
    }

    private void assertSavedRecord(AnswerStatus answerStatus, boolean matched, int retrievedChunkCount,
            int rawRetrievedChunkCount, String citationsJsonFragment) {
        ArgumentCaptor<ChatRecord> captor = ArgumentCaptor.forClass(ChatRecord.class);
        verify(chatRecordService).save(captor.capture());
        assertThat(captor.getValue().getAnswerStatus()).isEqualTo(answerStatus);
        assertThat(captor.getValue().getMatched()).isEqualTo(matched);
        assertThat(captor.getValue().getRetrievedChunkCount()).isEqualTo(retrievedChunkCount);
        assertThat(captor.getValue().getTopK()).isEqualTo(5);
        assertThat(captor.getValue().getRawRetrievedChunkCount()).isEqualTo(rawRetrievedChunkCount);
        assertThat(captor.getValue().getCitationsJson()).contains(citationsJsonFragment);
    }

    private Conversation conversation() {
        return conversation("conversation-1");
    }

    private Conversation conversation(String conversationUid) {
        Conversation conversation = new Conversation();
        conversation.setConversationUid(conversationUid);
        conversation.setUserId(USER_ID);
        conversation.setKnowledgeBaseId(KNOWLEDGE_BASE_ID);
        return conversation;
    }

    private ChatAskRequest request() {
        ChatAskRequest request = new ChatAskRequest();
        request.setKnowledgeBaseId(KNOWLEDGE_BASE_ID);
        request.setQuestion("question");
        return request;
    }

    private RetrievalSearchVO retrievalResult(List<RetrievalChunkVO> rawChunks, List<RetrievalChunkVO> effectiveChunks) {
        return RetrievalSearchVO.builder()
                .rawChunks(rawChunks)
                .effectiveChunks(effectiveChunks)
                .rawRetrievedChunkCount(rawChunks.size())
                .effectiveChunkCount(effectiveChunks.size())
                .minEffectiveScore(0.3D)
                .build();
    }

    private RetrievalChunkVO chunk(Double score) {
        return RetrievalChunkVO.builder()
                .chunkId(101L)
                .documentId(201L)
                .knowledgeBaseId(KNOWLEDGE_BASE_ID)
                .chunkIndex(1)
                .documentName("doc.txt")
                .content("matched content")
                .score(score)
                .build();
    }
}
