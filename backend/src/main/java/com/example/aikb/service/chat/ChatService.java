package com.example.aikb.service.chat;

import com.example.aikb.dto.chat.ChatAskRequest;
import com.example.aikb.vo.chat.ChatAskResponse;

/**
 * 问答服务边界，负责串联检索、答案生成和问答记录保存。
 */
public interface ChatService {

    /**
     * 执行一次知识库问答。
     *
     * @param request 提问请求
     * @return 问答结果
     */
    ChatAskResponse ask(ChatAskRequest request);
}
