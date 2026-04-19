package com.example.aikb.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aikb.entity.ChatRecord;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.ChatRecordMapper;
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.chat.ChatRecordQueryService;
import com.example.aikb.vo.chat.ChatRecordDetailVO;
import com.example.aikb.vo.chat.ChatRecordListItemVO;
import com.example.aikb.vo.chat.ChatRecordPageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 历史问答记录查询实现，所有查询都按当前登录用户隔离数据。
 */
@Service
@RequiredArgsConstructor
public class ChatRecordQueryServiceImpl implements ChatRecordQueryService {

    private static final int ANSWER_PREVIEW_LENGTH = 120;

    private final ChatRecordMapper chatRecordMapper;

    @Override
    public ChatRecordPageResponse page(Long knowledgeBaseId, long pageNum, long pageSize) {
        Long userId = CurrentUser.getUserId();
        Page<ChatRecord> page = Page.of(pageNum, pageSize);

        IPage<ChatRecord> result = chatRecordMapper.selectPage(page, new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getUserId, userId)
                .eq(knowledgeBaseId != null, ChatRecord::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(ChatRecord::getCreatedAt)
                .orderByDesc(ChatRecord::getId));

        List<ChatRecordListItemVO> list = result.getRecords()
                .stream()
                .map(this::toListItemVO)
                .toList();

        return ChatRecordPageResponse.builder()
                .list(list)
                .total(result.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public ChatRecordDetailVO getById(Long id) {
        Long userId = CurrentUser.getUserId();
        ChatRecord record = chatRecordMapper.selectOne(new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getId, id)
                .eq(ChatRecord::getUserId, userId)
                .last("LIMIT 1"));
        if (record == null) {
            throw new BusinessException(40400, "问答记录不存在");
        }
        return toDetailVO(record);
    }

    private ChatRecordListItemVO toListItemVO(ChatRecord record) {
        return ChatRecordListItemVO.builder()
                .id(record.getId())
                .knowledgeBaseId(record.getKnowledgeBaseId())
                .question(record.getQuestion())
                .answerPreview(preview(record.getAnswer()))
                .matched(record.getMatched())
                .retrievedChunkCount(record.getRetrievedChunkCount())
                .createdAt(record.getCreatedAt())
                .build();
    }

    private ChatRecordDetailVO toDetailVO(ChatRecord record) {
        return ChatRecordDetailVO.builder()
                .id(record.getId())
                .knowledgeBaseId(record.getKnowledgeBaseId())
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
