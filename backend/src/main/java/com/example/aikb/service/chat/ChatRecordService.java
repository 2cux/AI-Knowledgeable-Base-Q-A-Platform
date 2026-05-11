package com.example.aikb.service.chat;

import com.example.aikb.entity.ChatRecord;

/**
 * 问答记录服务，封装问答日志的保存逻辑。
 */
public interface ChatRecordService {

    /**
     * 保存一次问答记录。
     *
     * @param record 问答记录
     * @return 已保存的问答记录
     */
    ChatRecord save(ChatRecord record);
}
