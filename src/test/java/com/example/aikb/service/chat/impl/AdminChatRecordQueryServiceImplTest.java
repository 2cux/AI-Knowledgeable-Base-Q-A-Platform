package com.example.aikb.service.chat.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aikb.common.PageResult;
import com.example.aikb.dto.chat.AdminChatFeedbackQueryRow;
import com.example.aikb.dto.chat.AdminChatStatsCountRow;
import com.example.aikb.dto.chat.AdminFeedbackStatsRow;
import com.example.aikb.entity.ChatRecord;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.ChatFeedbackMapper;
import com.example.aikb.mapper.ChatRecordMapper;
import com.example.aikb.service.admin.AdminPermissionService;
import com.example.aikb.service.chat.AnswerStatus;
import com.example.aikb.vo.chat.AdminChatFeedbackVO;
import com.example.aikb.vo.chat.AdminChatRecordDetailVO;
import com.example.aikb.vo.chat.AdminChatStatsVO;
import com.example.aikb.vo.chat.AdminHotQuestionVO;
import com.example.aikb.vo.chat.AdminMissedQuestionVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminChatRecordQueryServiceImplTest {

    @Mock
    private ChatRecordMapper chatRecordMapper;

    @Mock
    private ChatFeedbackMapper chatFeedbackMapper;

    @Mock
    private AdminPermissionService adminPermissionService;

    private AdminChatRecordQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminChatRecordQueryServiceImpl(chatRecordMapper, chatFeedbackMapper, adminPermissionService,
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
    void pageMissedQuestionsReturnsMissedRecordsWithDefaultPageAndPreview() {
        ChatRecord record = record();
        record.setMatched(false);
        record.setAnswer("a".repeat(130));
        Page<ChatRecord> mapperPage = new Page<>(1, 10);
        mapperPage.setRecords(List.of(record));
        mapperPage.setTotal(1);
        when(chatRecordMapper.selectPage(any(), any())).thenReturn(mapperPage);

        PageResult<AdminMissedQuestionVO> result = service.pageMissedQuestions(null, null, null, null, null);

        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        AdminMissedQuestionVO item = result.getList().get(0);
        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getUserId()).isEqualTo(7L);
        assertThat(item.getConversationId()).isEqualTo("conversation-1");
        assertThat(item.getMatched()).isFalse();
        assertThat(item.getTopK()).isEqualTo(5);
        assertThat(item.getAnswerPreview()).hasSize(123).endsWith("...");
        verify(adminPermissionService).ensureAdmin();
    }

    @Test
    void pageMissedQuestionsLimitsOversizedPageSize() {
        Page<ChatRecord> mapperPage = new Page<>(1, 100);
        when(chatRecordMapper.selectPage(any(), any())).thenReturn(mapperPage);

        service.pageMissedQuestions(11L, null, null, 2L, 200L);

        ArgumentCaptor<Page<ChatRecord>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(chatRecordMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100);
    }

    @Test
    void pageMissedQuestionsThrowsWhenStartTimeAfterEndTime() {
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 3, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 2, 10, 0);

        assertThatThrownBy(() -> service.pageMissedQuestions(null, startTime, endTime, 1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("startTime");

        verify(adminPermissionService).ensureAdmin();
        verify(chatRecordMapper, never()).selectPage(any(), any());
    }

    @Test
    void pageMissedQuestionsRequiresAdminBeforeQueryingRecords() {
        doThrow(new BusinessException(40300, "forbidden"))
                .when(adminPermissionService).ensureAdmin();

        assertThatThrownBy(() -> service.pageMissedQuestions(null, null, null, 1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("forbidden");

        verify(chatRecordMapper, never()).selectPage(any(), any());
    }

    @Test
    void pageFeedbackReturnsFeedbackWithDefaultPageAndPreview() {
        AdminChatFeedbackQueryRow row = feedbackRow();
        row.setAnswer("a".repeat(130));
        Page<AdminChatFeedbackQueryRow> mapperPage = new Page<>(1, 10);
        mapperPage.setRecords(List.of(row));
        mapperPage.setTotal(1);
        when(chatFeedbackMapper.selectAdminFeedbackPage(any(), any(), any(), any(), any())).thenReturn(mapperPage);

        PageResult<AdminChatFeedbackVO> result = service.pageFeedback(null, null, null, null, null, null);

        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        AdminChatFeedbackVO item = result.getList().get(0);
        assertThat(item.getId()).isEqualTo(21L);
        assertThat(item.getChatRecordId()).isEqualTo(1L);
        assertThat(item.getUserId()).isEqualTo(7L);
        assertThat(item.getKnowledgeBaseId()).isEqualTo(11L);
        assertThat(item.getQuestion()).isEqualTo("question");
        assertThat(item.getRating()).isEqualTo("DISLIKE");
        assertThat(item.getComment()).isEqualTo("not helpful");
        assertThat(item.getAnswerPreview()).hasSize(123).endsWith("...");
        verify(adminPermissionService).ensureAdmin();
    }

    @Test
    void pageFeedbackLimitsOversizedPageSize() {
        Page<AdminChatFeedbackQueryRow> mapperPage = new Page<>(1, 100);
        when(chatFeedbackMapper.selectAdminFeedbackPage(any(), any(), any(), any(), any())).thenReturn(mapperPage);

        service.pageFeedback(11L, " like ", null, null, 2L, 200L);

        ArgumentCaptor<Page<AdminChatFeedbackQueryRow>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        ArgumentCaptor<String> feedbackTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatFeedbackMapper).selectAdminFeedbackPage(pageCaptor.capture(), any(), feedbackTypeCaptor.capture(),
                any(), any());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100);
        assertThat(feedbackTypeCaptor.getValue()).isEqualTo("LIKE");
    }

    @Test
    void pageFeedbackReturnsEmptyListWhenNoFeedbackExists() {
        Page<AdminChatFeedbackQueryRow> mapperPage = new Page<>(1, 10);
        mapperPage.setRecords(List.of());
        mapperPage.setTotal(0);
        when(chatFeedbackMapper.selectAdminFeedbackPage(any(), any(), any(), any(), any())).thenReturn(mapperPage);

        PageResult<AdminChatFeedbackVO> result = service.pageFeedback(null, null, null, null, null, null);

        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotal()).isZero();
        assertThat(result.getList()).isEmpty();
    }

    @Test
    void pageFeedbackReturnsEmptyAnswerPreviewWhenAnswerIsNull() {
        AdminChatFeedbackQueryRow row = feedbackRow();
        row.setAnswer(null);
        Page<AdminChatFeedbackQueryRow> mapperPage = new Page<>(1, 10);
        mapperPage.setRecords(List.of(row));
        mapperPage.setTotal(1);
        when(chatFeedbackMapper.selectAdminFeedbackPage(any(), any(), any(), any(), any())).thenReturn(mapperPage);

        PageResult<AdminChatFeedbackVO> result = service.pageFeedback(null, null, null, null, null, null);

        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getAnswerPreview()).isEmpty();
    }

    @Test
    void pageFeedbackThrowsWhenRatingInvalid() {
        assertThatThrownBy(() -> service.pageFeedback(null, "BAD", null, null, 1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rating");

        verify(adminPermissionService).ensureAdmin();
        verify(chatFeedbackMapper, never()).selectAdminFeedbackPage(any(), any(), any(), any(), any());
    }

    @Test
    void pageFeedbackThrowsWhenStartTimeAfterEndTime() {
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 3, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 2, 10, 0);

        assertThatThrownBy(() -> service.pageFeedback(null, null, startTime, endTime, 1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("startTime");

        verify(adminPermissionService).ensureAdmin();
        verify(chatFeedbackMapper, never()).selectAdminFeedbackPage(any(), any(), any(), any(), any());
    }

    @Test
    void pageFeedbackRequiresAdminBeforeQueryingFeedback() {
        doThrow(new BusinessException(40300, "forbidden"))
                .when(adminPermissionService).ensureAdmin();

        assertThatThrownBy(() -> service.pageFeedback(null, null, null, null, 1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("forbidden");

        verify(chatFeedbackMapper, never()).selectAdminFeedbackPage(any(), any(), any(), any(), any());
    }

    @Test
    void listHotQuestionsReturnsTopItemsAndCapsLimit() {
        AdminHotQuestionVO hotQuestion = AdminHotQuestionVO.builder()
                .question("question")
                .count(3L)
                .latestAskedAt(LocalDateTime.of(2026, 5, 2, 12, 0))
                .build();
        when(chatRecordMapper.selectHotQuestions(any(), any(), any(), anyInt()))
                .thenReturn(List.of(hotQuestion));

        List<AdminHotQuestionVO> result = service.listHotQuestions(11L, null, null, 200);

        assertThat(result).containsExactly(hotQuestion);
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(chatRecordMapper).selectHotQuestions(any(), any(), any(), limitCaptor.capture());
        assertThat(limitCaptor.getValue()).isEqualTo(50);
        verify(adminPermissionService).ensureAdmin();
    }

    @Test
    void listHotQuestionsUsesDefaultLimitWhenLimitIsTooSmall() {
        when(chatRecordMapper.selectHotQuestions(any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        service.listHotQuestions(null, null, null, 0);

        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(chatRecordMapper).selectHotQuestions(any(), any(), any(), limitCaptor.capture());
        assertThat(limitCaptor.getValue()).isEqualTo(10);
    }

    @Test
    void listHotQuestionsThrowsWhenStartTimeAfterEndTime() {
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 3, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 2, 10, 0);

        assertThatThrownBy(() -> service.listHotQuestions(null, startTime, endTime, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("startTime");

        verify(adminPermissionService).ensureAdmin();
        verify(chatRecordMapper, never()).selectHotQuestions(any(), any(), any(), anyInt());
    }

    @Test
    void getStatsReturnsCountsAndMatchRate() {
        AdminChatStatsCountRow chatStats = new AdminChatStatsCountRow();
        chatStats.setTotalChatCount(10L);
        chatStats.setMatchedCount(8L);
        chatStats.setMissedCount(2L);
        AdminFeedbackStatsRow feedbackStats = new AdminFeedbackStatsRow();
        feedbackStats.setFeedbackCount(5L);
        feedbackStats.setLikeCount(3L);
        feedbackStats.setDislikeCount(2L);
        when(chatRecordMapper.selectAdminChatStats(any(), any(), any())).thenReturn(chatStats);
        when(chatFeedbackMapper.selectAdminFeedbackStats(any(), any(), any())).thenReturn(feedbackStats);

        AdminChatStatsVO result = service.getStats(11L, null, null);

        assertThat(result.getTotalChatCount()).isEqualTo(10L);
        assertThat(result.getMatchedCount()).isEqualTo(8L);
        assertThat(result.getMissedCount()).isEqualTo(2L);
        assertThat(result.getMatchRate()).isEqualByComparingTo(new BigDecimal("0.8000"));
        assertThat(result.getFeedbackCount()).isEqualTo(5L);
        assertThat(result.getLikeCount()).isEqualTo(3L);
        assertThat(result.getDislikeCount()).isEqualTo(2L);
        verify(adminPermissionService).ensureAdmin();
    }

    @Test
    void getStatsReturnsZeroMatchRateWhenNoChatsExist() {
        AdminChatStatsCountRow chatStats = new AdminChatStatsCountRow();
        chatStats.setTotalChatCount(0L);
        chatStats.setMatchedCount(0L);
        chatStats.setMissedCount(0L);
        when(chatRecordMapper.selectAdminChatStats(any(), any(), any())).thenReturn(chatStats);
        when(chatFeedbackMapper.selectAdminFeedbackStats(any(), any(), any())).thenReturn(null);

        AdminChatStatsVO result = service.getStats(null, null, null);

        assertThat(result.getTotalChatCount()).isZero();
        assertThat(result.getMatchRate()).isEqualByComparingTo(new BigDecimal("0.0000"));
        assertThat(result.getFeedbackCount()).isZero();
        assertThat(result.getLikeCount()).isZero();
        assertThat(result.getDislikeCount()).isZero();
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

    private AdminChatFeedbackQueryRow feedbackRow() {
        AdminChatFeedbackQueryRow row = new AdminChatFeedbackQueryRow();
        row.setId(21L);
        row.setChatRecordId(1L);
        row.setUserId(7L);
        row.setKnowledgeBaseId(11L);
        row.setQuestion("question");
        row.setAnswer("answer");
        row.setFeedbackType("DISLIKE");
        row.setComment("not helpful");
        row.setCreatedAt(LocalDateTime.of(2026, 5, 2, 11, 30));
        return row;
    }
}
