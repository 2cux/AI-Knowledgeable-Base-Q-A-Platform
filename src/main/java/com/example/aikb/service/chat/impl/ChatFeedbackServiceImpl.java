package com.example.aikb.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aikb.dto.chat.ChatFeedbackRequest;
import com.example.aikb.entity.ChatFeedback;
import com.example.aikb.entity.ChatRecord;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.ChatFeedbackMapper;
import com.example.aikb.mapper.ChatRecordMapper;
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.chat.ChatFeedbackService;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 问答反馈服务实现，确保用户只能反馈自己的问答记录。
 */
@Service
@RequiredArgsConstructor
public class ChatFeedbackServiceImpl implements ChatFeedbackService {

    private static final Set<String> SUPPORTED_FEEDBACK_TYPES = Set.of("LIKE", "DISLIKE");

    private final ChatRecordMapper chatRecordMapper;
    private final ChatFeedbackMapper chatFeedbackMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long chatRecordId, ChatFeedbackRequest request) {
        if (request == null) {
            throw new BusinessException(40001, "反馈请求不能为空");
        }
        Long userId = CurrentUser.getUserId();
        ChatRecord chatRecord = getOwnChatRecord(chatRecordId, userId);
        String feedbackType = normalizeFeedbackType(request.getFeedbackType());
        ensureNotSubmitted(chatRecord.getId(), userId);

        ChatFeedback feedback = new ChatFeedback();
        feedback.setChatRecordId(chatRecord.getId());
        feedback.setUserId(userId);
        feedback.setFeedbackType(feedbackType);
        feedback.setComment(trimToNull(request.getComment()));
        feedback.setCreatedAt(LocalDateTime.now());

        try {
            chatFeedbackMapper.insert(feedback);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(40900, "该问答记录已提交反馈");
        }
    }

    private ChatRecord getOwnChatRecord(Long chatRecordId, Long userId) {
        ChatRecord chatRecord = chatRecordMapper.selectOne(new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getId, chatRecordId)
                .eq(ChatRecord::getUserId, userId)
                .last("LIMIT 1"));
        if (chatRecord == null) {
            throw new BusinessException(40400, "问答记录不存在");
        }
        return chatRecord;
    }

    private String normalizeFeedbackType(String feedbackType) {
        String normalized = feedbackType == null ? null : feedbackType.trim().toUpperCase();
        if (!SUPPORTED_FEEDBACK_TYPES.contains(normalized)) {
            throw new BusinessException(40001, "feedbackType只支持LIKE或DISLIKE");
        }
        return normalized;
    }

    private void ensureNotSubmitted(Long chatRecordId, Long userId) {
        Long count = chatFeedbackMapper.selectCount(new LambdaQueryWrapper<ChatFeedback>()
                .eq(ChatFeedback::getChatRecordId, chatRecordId)
                .eq(ChatFeedback::getUserId, userId));
        if (count != null && count > 0) {
            throw new BusinessException(40900, "该问答记录已提交反馈");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
