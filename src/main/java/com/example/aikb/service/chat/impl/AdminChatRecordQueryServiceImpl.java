package com.example.aikb.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aikb.common.PageResult;
import com.example.aikb.entity.ChatRecord;
import com.example.aikb.entity.User;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.ChatRecordMapper;
import com.example.aikb.mapper.UserMapper;
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.chat.AdminChatRecordQueryService;
import com.example.aikb.vo.chat.AdminChatRecordDetailVO;
import com.example.aikb.vo.chat.AdminChatRecordListItemVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 管理端问答日志查询实现。MVP阶段直接复用 chat_record 表。
 */
@Service
@RequiredArgsConstructor
public class AdminChatRecordQueryServiceImpl implements AdminChatRecordQueryService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final int ANSWER_PREVIEW_LENGTH = 120;

    private final ChatRecordMapper chatRecordMapper;
    private final UserMapper userMapper;

    @Override
    public PageResult<AdminChatRecordListItemVO> page(Long knowledgeBaseId, Boolean matched, long pageNum,
            long pageSize) {
        ensureAdmin();

        Page<ChatRecord> page = Page.of(pageNum, pageSize);
        IPage<ChatRecord> result = chatRecordMapper.selectPage(page, new LambdaQueryWrapper<ChatRecord>()
                .eq(knowledgeBaseId != null, ChatRecord::getKnowledgeBaseId, knowledgeBaseId)
                .eq(matched != null, ChatRecord::getMatched, matched)
                .orderByDesc(ChatRecord::getCreatedAt)
                .orderByDesc(ChatRecord::getId));

        List<AdminChatRecordListItemVO> list = result.getRecords()
                .stream()
                .map(this::toListItemVO)
                .toList();

        return PageResult.<AdminChatRecordListItemVO>builder()
                .list(list)
                .total(result.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public AdminChatRecordDetailVO getById(Long id) {
        ensureAdmin();

        ChatRecord record = chatRecordMapper.selectOne(new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getId, id)
                .last("LIMIT 1"));
        if (record == null) {
            throw new BusinessException(40400, "问答日志不存在");
        }
        return toDetailVO(record);
    }

    private void ensureAdmin() {
        Long userId = CurrentUser.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1
                || !ADMIN_ROLE.equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(40300, "无权访问管理端问答日志");
        }
    }

    private AdminChatRecordListItemVO toListItemVO(ChatRecord record) {
        return AdminChatRecordListItemVO.builder()
                .id(record.getId())
                .knowledgeBaseId(record.getKnowledgeBaseId())
                .question(record.getQuestion())
                .answerPreview(preview(record.getAnswer()))
                .matched(record.getMatched())
                .retrievedChunkCount(record.getRetrievedChunkCount())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private AdminChatRecordDetailVO toDetailVO(ChatRecord record) {
        return AdminChatRecordDetailVO.builder()
                .id(record.getId())
                .knowledgeBaseId(record.getKnowledgeBaseId())
                .conversationId(record.getConversationId())
                .question(record.getQuestion())
                .answer(record.getAnswer())
                .matched(record.getMatched())
                .retrievedChunkCount(record.getRetrievedChunkCount())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private String preview(String answer) {
        if (answer == null || answer.length() <= ANSWER_PREVIEW_LENGTH) {
            return answer;
        }
        return answer.substring(0, ANSWER_PREVIEW_LENGTH) + "...";
    }
}
