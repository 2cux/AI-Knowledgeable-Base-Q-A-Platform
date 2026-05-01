package com.example.aikb.service.chat;

import com.example.aikb.common.PageResult;
import com.example.aikb.entity.Conversation;
import com.example.aikb.vo.chat.ConversationDetailVO;
import com.example.aikb.vo.chat.ConversationListItemVO;

public interface ConversationService {

    /**
     * Creates a conversation for first ask or validates an existing one.
     */
    Conversation resolveForAsk(Long userId, Long knowledgeBaseId, String conversationId, String question);

    /**
     * Updates summary fields after a successful ask persistence.
     */
    void touchAfterAsk(String conversationUid, String question, String answer, int appendedMessageCount);

    PageResult<ConversationListItemVO> pageCurrentUser(Long knowledgeBaseId, long pageNum, long pageSize);

    ConversationDetailVO getCurrentUserDetail(String conversationId);
}
