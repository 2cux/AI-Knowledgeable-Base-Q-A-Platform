package com.example.aikb.vo.admin;

import com.example.aikb.vo.chat.AdminChatRecordListItemVO;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardVO {

    private Long knowledgeBaseCount;

    private Long documentCount;

    private Long chatRecordCount;

    private Long feedbackCount;

    private Long unmatchedQuestionCount;

    private List<AdminChatRecordListItemVO> recentChatRecords;
}
