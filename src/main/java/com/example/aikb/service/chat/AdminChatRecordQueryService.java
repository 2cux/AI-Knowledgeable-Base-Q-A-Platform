package com.example.aikb.service.chat;

import com.example.aikb.common.PageResult;
import com.example.aikb.vo.chat.AdminChatFeedbackVO;
import com.example.aikb.vo.chat.AdminChatRecordDetailVO;
import com.example.aikb.vo.chat.AdminChatRecordListItemVO;
import com.example.aikb.vo.chat.AdminMissedQuestionVO;
import java.time.LocalDateTime;

/**
 * 管理端问答日志查询服务。
 */
public interface AdminChatRecordQueryService {

    /**
     * 分页查询系统问答日志。
     */
    PageResult<AdminChatRecordListItemVO> page(Long knowledgeBaseId, Boolean matched, long pageNum, long pageSize);

    /**
     * Page missed questions for admin governance.
     */
    PageResult<AdminMissedQuestionVO> pageMissedQuestions(Long knowledgeBaseId, LocalDateTime startTime,
            LocalDateTime endTime, Long page, Long size);

    /**
     * Page chat feedback for admin operation.
     */
    PageResult<AdminChatFeedbackVO> pageFeedback(Long knowledgeBaseId, String rating, LocalDateTime startTime,
            LocalDateTime endTime, Long page, Long size);

    /**
     * 查询单条问答日志详情。
     */
    AdminChatRecordDetailVO getById(Long id);
}
