package com.example.aikb.service.chat.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.aikb.entity.ChatRecord;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.ChatRecordMapper;
import com.example.aikb.service.admin.AdminPermissionService;
import com.example.aikb.service.chat.AnswerStatus;
import com.example.aikb.vo.chat.AdminChatRecordDetailVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminChatRecordQueryServiceImplTest {

    @Mock
    private ChatRecordMapper chatRecordMapper;

    @Mock
    private AdminPermissionService adminPermissionService;

    private AdminChatRecordQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminChatRecordQueryServiceImpl(chatRecordMapper, adminPermissionService,
                new CitationJsonCodec(new ObjectMapper()));
    }

    @Test
    void getByIdReturnsDetailWithParsedCitations() {
        ChatRecord record = record();
        record.setCitationsJson("""
                [{"chunkId":101,"documentId":201,"documentName":"manual.pdf","contentSnippet":"preview","score":0.87}]
                """);
        when(chatRecordMapper.selectOne(any())).thenReturn(record);

        AdminChatRecordDetailVO detail = service.getById(1L);

        assertThat(detail.getId()).isEqualTo(1L);
        assertThat(detail.getUserId()).isEqualTo(7L);
        assertThat(detail.getKnowledgeBaseId()).isEqualTo(11L);
        assertThat(detail.getConversationId()).isEqualTo("conversation-1");
        assertThat(detail.getQuestion()).isEqualTo("question");
        assertThat(detail.getAnswer()).isEqualTo("answer");
        assertThat(detail.getMatched()).isTrue();
        assertThat(detail.getTopK()).isEqualTo(5);
        assertThat(detail.getRetrievedChunkCount()).isEqualTo(1);
        assertThat(detail.getRawRetrievedChunkCount()).isEqualTo(3);
        assertThat(detail.getCitations()).hasSize(1);
        assertThat(detail.getCitations().get(0).getDocumentName()).isEqualTo("manual.pdf");
        assertThat(detail.getCitations().get(0).getContentSnippet()).isEqualTo("preview");
        assertThat(detail.getCreatedAt()).isEqualTo(record.getCreatedAt());
        verify(adminPermissionService).ensureAdmin();
    }

    @Test
    void getByIdReturnsEmptyCitationsWhenJsonIsBlank() {
        ChatRecord record = record();
        record.setCitationsJson(null);
        when(chatRecordMapper.selectOne(any())).thenReturn(record);

        AdminChatRecordDetailVO detail = service.getById(1L);

        assertThat(detail.getCitations()).isEmpty();
    }

    @Test
    void getByIdReturnsEmptyCitationsWhenJsonIsInvalid() {
        ChatRecord record = record();
        record.setCitationsJson("not-json");
        when(chatRecordMapper.selectOne(any())).thenReturn(record);

        AdminChatRecordDetailVO detail = service.getById(1L);

        assertThat(detail.getCitations()).isEmpty();
    }

    @Test
    void getByIdThrowsWhenRecordDoesNotExist() {
        when(chatRecordMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    void getByIdThrowsParameterErrorBeforeQueryWhenIdIsInvalid() {
        assertThatThrownBy(() -> service.getById(0L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("问答记录ID必须大于0");

        verify(adminPermissionService, never()).ensureAdmin();
        verify(chatRecordMapper, never()).selectOne(any());
    }

    @Test
    void getByIdRequiresAdminBeforeQueryingRecord() {
        doThrow(new BusinessException(40300, "无权访问管理端接口"))
                .when(adminPermissionService).ensureAdmin();

        assertThatThrownBy(() -> service.getById(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权访问管理端接口");

        verify(chatRecordMapper, never()).selectOne(any());
    }

    private ChatRecord record() {
        ChatRecord record = new ChatRecord();
        record.setId(1L);
        record.setUserId(7L);
        record.setKnowledgeBaseId(11L);
        record.setConversationId("conversation-1");
        record.setQuestion("question");
        record.setAnswer("answer");
        record.setAnswerStatus(AnswerStatus.SUCCESS);
        record.setMatched(true);
        record.setRetrievedChunkCount(1);
        record.setRawRetrievedChunkCount(3);
        record.setTopK(5);
        record.setCreatedAt(LocalDateTime.of(2026, 5, 2, 10, 30));
        return record;
    }
}
