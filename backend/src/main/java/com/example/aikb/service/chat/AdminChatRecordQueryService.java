package com.example.aikb.service.chat;

import com.example.aikb.common.PageResult;
import com.example.aikb.vo.chat.AdminChatFeedbackVO;
import com.example.aikb.vo.chat.AdminChatRecordDetailVO;
import com.example.aikb.vo.chat.AdminChatRecordListItemVO;
import com.example.aikb.vo.chat.AdminChatStatsVO;
import com.example.aikb.vo.chat.AdminHotQuestionVO;
import com.example.aikb.vo.chat.AdminMissedQuestionVO;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin-side query service for chat records, feedback, and operation stats.
 */
public interface AdminChatRecordQueryService {

    PageResult<AdminChatRecordListItemVO> page(Long knowledgeBaseId, Boolean matched, String answerStatus,
            String keyword, long pageNum, long pageSize);

    PageResult<AdminMissedQuestionVO> pageMissedQuestions(Long knowledgeBaseId, LocalDateTime startTime,
            LocalDateTime endTime, Long page, Long size);

    PageResult<AdminChatFeedbackVO> pageFeedback(Long knowledgeBaseId, String rating, String reason,
            LocalDateTime startTime, LocalDateTime endTime, Long page, Long size);

    List<AdminHotQuestionVO> listHotQuestions(Long knowledgeBaseId, LocalDateTime startTime,
            LocalDateTime endTime, Integer limit);

    AdminChatStatsVO getStats(Long knowledgeBaseId, LocalDateTime startTime, LocalDateTime endTime);

    AdminChatRecordDetailVO getById(Long id);

    PageResult<AdminMissedQuestionVO> pageUnmatchedQuestions(Long knowledgeBaseId, String keyword, Long page,
            Long size);
}
