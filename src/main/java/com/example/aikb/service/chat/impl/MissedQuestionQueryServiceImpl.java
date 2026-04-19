package com.example.aikb.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aikb.common.PageResult;
import com.example.aikb.entity.ChatRecord;
import com.example.aikb.mapper.ChatRecordMapper;
import com.example.aikb.service.admin.AdminPermissionService;
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

    private final ChatRecordMapper chatRecordMapper;
    private final AdminPermissionService adminPermissionService;

    @Override
    public PageResult<MissedQuestionItemVO> page(Long knowledgeBaseId, long pageNum, long pageSize) {
        adminPermissionService.ensureAdmin();

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
