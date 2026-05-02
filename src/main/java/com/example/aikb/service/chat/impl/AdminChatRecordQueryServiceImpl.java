package com.example.aikb.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aikb.common.PageResult;
import com.example.aikb.entity.ChatRecord;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.ChatRecordMapper;
import com.example.aikb.service.admin.AdminPermissionService;
import com.example.aikb.service.chat.AdminChatRecordQueryService;
import com.example.aikb.vo.chat.AdminChatRecordDetailVO;
import com.example.aikb.vo.chat.AdminChatRecordListItemVO;
import com.example.aikb.vo.chat.AdminMissedQuestionVO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 管理端问答日志查询实现。MVP阶段直接复用 chat_record 表。
 */
@Service
@RequiredArgsConstructor
public class AdminChatRecordQueryServiceImpl implements AdminChatRecordQueryService {

    private static final int ANSWER_PREVIEW_LENGTH = 120;
    private static final long DEFAULT_PAGE_NUM = 1L;
    private static final long DEFAULT_PAGE_SIZE = 10L;
    private static final long MAX_PAGE_SIZE = 100L;

    private final ChatRecordMapper chatRecordMapper;
    private final AdminPermissionService adminPermissionService;
    private final CitationJsonCodec citationJsonCodec;

    @Override
    public PageResult<AdminChatRecordListItemVO> page(Long knowledgeBaseId, Boolean matched, long pageNum,
            long pageSize) {
        adminPermissionService.ensureAdmin();

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
    public PageResult<AdminMissedQuestionVO> pageMissedQuestions(Long knowledgeBaseId, LocalDateTime startTime,
            LocalDateTime endTime, Long page, Long size) {
        adminPermissionService.ensureAdmin();
        if (knowledgeBaseId != null && knowledgeBaseId <= 0) {
            throw new BusinessException(40001, "knowledgeBaseId must be greater than 0");
        }
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException(40001, "startTime cannot be later than endTime");
        }

        long pageNum = normalizePageNum(page);
        long pageSize = normalizePageSize(size);
        Page<ChatRecord> pageRequest = Page.of(pageNum, pageSize);
        IPage<ChatRecord> result = chatRecordMapper.selectPage(pageRequest, new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getMatched, false)
                .eq(knowledgeBaseId != null, ChatRecord::getKnowledgeBaseId, knowledgeBaseId)
                .ge(startTime != null, ChatRecord::getCreatedAt, startTime)
                .le(endTime != null, ChatRecord::getCreatedAt, endTime)
                .orderByDesc(ChatRecord::getCreatedAt)
                .orderByDesc(ChatRecord::getId));

        List<AdminMissedQuestionVO> list = result.getRecords()
                .stream()
                .map(this::toMissedQuestionVO)
                .toList();

        return PageResult.<AdminMissedQuestionVO>builder()
                .list(list)
                .total(result.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public AdminChatRecordDetailVO getById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(40001, "问答记录ID必须大于0");
        }
        adminPermissionService.ensureAdmin();

        ChatRecord record = chatRecordMapper.selectOne(new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getId, id)
                .last("LIMIT 1"));
        if (record == null) {
            throw new BusinessException(40400, "问答日志不存在");
        }
        return toDetailVO(record);
    }

    private AdminChatRecordListItemVO toListItemVO(ChatRecord record) {
        return AdminChatRecordListItemVO.builder()
                .id(record.getId())
                .knowledgeBaseId(record.getKnowledgeBaseId())
                .question(record.getQuestion())
                .answerPreview(preview(record.getAnswer()))
                .answerStatus(record.getAnswerStatus())
                .matched(record.getMatched())
                .retrievedChunkCount(record.getRetrievedChunkCount())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private AdminMissedQuestionVO toMissedQuestionVO(ChatRecord record) {
        return AdminMissedQuestionVO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .knowledgeBaseId(record.getKnowledgeBaseId())
                .conversationId(record.getConversationId())
                .question(record.getQuestion())
                .answerPreview(preview(record.getAnswer()))
                .matched(record.getMatched())
                .retrievedChunkCount(record.getRetrievedChunkCount())
                .topK(record.getTopK())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private AdminChatRecordDetailVO toDetailVO(ChatRecord record) {
        return AdminChatRecordDetailVO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .knowledgeBaseId(record.getKnowledgeBaseId())
                .conversationId(record.getConversationId())
                .question(record.getQuestion())
                .answer(record.getAnswer())
                .answerStatus(record.getAnswerStatus())
                .matched(record.getMatched())
                .retrievedChunkCount(record.getRetrievedChunkCount())
                .rawRetrievedChunkCount(record.getRawRetrievedChunkCount())
                .topK(record.getTopK())
                .citations(citationJsonCodec.deserialize(record.getCitationsJson(), record.getId()))
                .createdAt(record.getCreatedAt())
                .build();
    }

    private String preview(String answer) {
        if (answer == null || answer.isEmpty()) {
            return "";
        }
        if (answer.length() <= ANSWER_PREVIEW_LENGTH) {
            return answer;
        }
        return answer.substring(0, ANSWER_PREVIEW_LENGTH) + "...";
    }

    private long normalizePageNum(Long page) {
        if (page == null || page < DEFAULT_PAGE_NUM) {
            return DEFAULT_PAGE_NUM;
        }
        return page;
    }

    private long normalizePageSize(Long size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
