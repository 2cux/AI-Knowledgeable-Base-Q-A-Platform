package com.example.aikb.dto.chat;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 管理端反馈列表查询行，承接 chat_feedback 与 chat_record 的关联结果。
 */
@Data
public class AdminChatFeedbackQueryRow {

    private Long id;
    private Long chatRecordId;
    private Long userId;
    private Long knowledgeBaseId;
    private String question;
    private String answer;
    private String feedbackType;
    private String comment;
    private LocalDateTime createdAt;
}
