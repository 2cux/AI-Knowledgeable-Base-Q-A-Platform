package com.example.aikb.service.admin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aikb.common.PageResult;
import com.example.aikb.entity.Document;
import com.example.aikb.entity.KnowledgeBase;
import com.example.aikb.mapper.ChatFeedbackMapper;
import com.example.aikb.mapper.ChatRecordMapper;
import com.example.aikb.mapper.DocumentMapper;
import com.example.aikb.mapper.KnowledgeBaseMapper;
import com.example.aikb.service.admin.AdminDashboardService;
import com.example.aikb.service.admin.AdminPermissionService;
import com.example.aikb.service.chat.AdminChatRecordQueryService;
import com.example.aikb.service.chat.AnswerStatus;
import com.example.aikb.vo.admin.AdminDashboardVO;
import com.example.aikb.vo.chat.AdminChatRecordListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final long RECENT_CHAT_LIMIT = 8L;

    private final AdminPermissionService adminPermissionService;
    private final AdminChatRecordQueryService adminChatRecordQueryService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentMapper documentMapper;
    private final ChatRecordMapper chatRecordMapper;
    private final ChatFeedbackMapper chatFeedbackMapper;

    @Override
    public AdminDashboardVO getDashboard() {
        adminPermissionService.ensureAdmin();

        PageResult<AdminChatRecordListItemVO> recentChats =
                adminChatRecordQueryService.page(null, null, null, null, 1L, RECENT_CHAT_LIMIT);

        return AdminDashboardVO.builder()
                .knowledgeBaseCount(countKnowledgeBases())
                .documentCount(documentMapper.selectCount(new LambdaQueryWrapper<Document>()))
                .chatRecordCount(chatRecordMapper.selectCount(null))
                .feedbackCount(chatFeedbackMapper.selectCount(null))
                .unmatchedQuestionCount(countUnmatchedQuestions())
                .recentChatRecords(recentChats.getList())
                .build();
    }

    private Long countKnowledgeBases() {
        return knowledgeBaseMapper.selectCount(new LambdaQueryWrapper<KnowledgeBase>()
                .and(wrapper -> wrapper
                        .isNull(KnowledgeBase::getDeleted)
                        .or()
                        .ne(KnowledgeBase::getDeleted, 1)));
    }

    private Long countUnmatchedQuestions() {
        return chatRecordMapper.selectCount(new LambdaQueryWrapper<com.example.aikb.entity.ChatRecord>()
                .and(wrapper -> wrapper
                        .eq(com.example.aikb.entity.ChatRecord::getMatched, false)
                        .or()
                        .eq(com.example.aikb.entity.ChatRecord::getRetrievedChunkCount, 0)
                        .or()
                        .in(com.example.aikb.entity.ChatRecord::getAnswerStatus, AnswerStatus.NO_HIT,
                                AnswerStatus.WEAK_HIT, AnswerStatus.RETRIEVAL_UNAVAILABLE)));
    }
}
