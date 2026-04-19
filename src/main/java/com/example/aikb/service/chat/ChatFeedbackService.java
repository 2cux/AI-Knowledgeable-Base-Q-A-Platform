package com.example.aikb.service.chat;

import com.example.aikb.dto.chat.ChatFeedbackRequest;

/**
 * 问答反馈服务，负责校验问答记录归属并保存用户反馈。
 */
public interface ChatFeedbackService {

    /**
     * 为当前用户自己的问答记录提交反馈。
     *
     * @param chatRecordId 问答记录ID
     * @param request 反馈请求参数
     */
    void submit(Long chatRecordId, ChatFeedbackRequest request);
}
