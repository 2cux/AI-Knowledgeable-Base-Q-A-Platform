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
import com.example.aikb.service.chat.MissedQuestionQueryService;
import com.example.aikb.vo.chat.MissedQuestionItemVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 未命中问题查询实现。MVP阶段直接复用 chat_record，不单独维护 missed_question 表。
 */
@Service
@RequiredArgsConstructor
public class MissedQuestionQueryServiceImpl implements MissedQuestionQueryService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final ChatRecordMapper chatRecordMapper;
    private final UserMapper userMapper;

    @Override
    public PageResult<MissedQuestionItemVO> page(Long knowledgeBaseId, long pageNum, long pageSize) {
        ensureAdmin();

        Page<ChatRecord> page = Page.of(pageNum, pageSize);
        IPage<ChatRecord> result = chatRecordMapper.selectPage(page, new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getMatched, false)
                .eq(knowledgeBaseId != null, ChatRecord::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(ChatRecord::getCreatedAt)
                .orderByDesc(ChatRecord::getId));

        List<MissedQuestionItemVO> list = result.getRecords()
                .stream()
                .map(this::toVO)
                .toList();

        return PageResult.<MissedQuestionItemVO>builder()
                .list(list)
                .total(result.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
    }

    private void ensureAdmin() {
        Long userId = CurrentUser.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null || !ADMIN_ROLE.equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(40300, "无权访问管理端未命中问题");
        }
    }

    private MissedQuestionItemVO toVO(ChatRecord record) {
        return MissedQuestionItemVO.builder()
                .id(record.getId())
                .knowledgeBaseId(record.getKnowledgeBaseId())
                .question(record.getQuestion())
                .retrievedChunkCount(record.getRetrievedChunkCount())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
