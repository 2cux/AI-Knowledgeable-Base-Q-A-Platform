package com.example.aikb.service.chat.impl;

import com.example.aikb.entity.ChatRecord;
import com.example.aikb.mapper.ChatRecordMapper;
import com.example.aikb.service.chat.ChatRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 问答记录服务实现，MVP阶段只负责落库保存。
 */
@Service
@RequiredArgsConstructor
public class ChatRecordServiceImpl implements ChatRecordService {

    private final ChatRecordMapper chatRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatRecord save(ChatRecord record) {
        chatRecordMapper.insert(record);
        return record;
    }
}
