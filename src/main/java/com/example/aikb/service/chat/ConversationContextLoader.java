package com.example.aikb.service.chat;

public interface ConversationContextLoader {

    /**
     * Loads recent same-user, same-knowledge-base messages as prompt-ready context text.
     */
    String load(Long userId, Long knowledgeBaseId, String conversationId);
}
