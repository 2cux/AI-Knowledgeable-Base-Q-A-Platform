package com.example.aikb.service.chat;

import com.example.aikb.entity.Message;
import com.example.aikb.vo.chat.CitationVO;
import java.util.List;

public interface MessageService {

    Message saveUserMessage(Long userId, Long knowledgeBaseId, String conversationUid, String content);

    Message saveAssistantMessage(Long userId, Long knowledgeBaseId, String conversationUid, String content,
            List<CitationVO> citations, Long chatRecordId);

    List<Message> listByConversation(String conversationUid, Long userId, Long knowledgeBaseId);

    List<Message> listRecentContextMessages(String conversationUid, Long userId, Long knowledgeBaseId, int limit);
}
