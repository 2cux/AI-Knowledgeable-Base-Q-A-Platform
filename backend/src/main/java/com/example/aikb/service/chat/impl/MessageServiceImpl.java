package com.example.aikb.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aikb.entity.Message;
import com.example.aikb.mapper.MessageMapper;
import com.example.aikb.service.chat.MessageService;
import com.example.aikb.vo.chat.CitationVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ASSISTANT = "ASSISTANT";
    private static final String SCOPE_ENTERPRISE_ALL = "ENTERPRISE_ALL";
    private static final String SCOPE_KNOWLEDGE_BASE = "KNOWLEDGE_BASE";

    private final MessageMapper messageMapper;
    private final CitationJsonCodec citationJsonCodec;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Message saveUserMessage(Long userId, Long knowledgeBaseId, String conversationUid, String content) {
        Message message = baseMessage(userId, knowledgeBaseId, conversationUid, ROLE_USER, content);
        message.setCitations(citationJsonCodec.serialize(Collections.emptyList()));
        messageMapper.insert(message);
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Message saveAssistantMessage(Long userId, Long knowledgeBaseId, String conversationUid, String content,
            List<CitationVO> citations, Long chatRecordId) {
        Message message = baseMessage(userId, knowledgeBaseId, conversationUid, ROLE_ASSISTANT, content);
        message.setCitations(citationJsonCodec.serialize(citations));
        message.setChatRecordId(chatRecordId);
        messageMapper.insert(message);
        return message;
    }

    @Override
    public List<Message> listByConversation(String conversationUid, Long userId, Long knowledgeBaseId) {
        return messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationUid, conversationUid)
                .eq(Message::getUserId, userId)
                .eq(knowledgeBaseId != null, Message::getKnowledgeBaseId, knowledgeBaseId)
                .isNull(knowledgeBaseId == null, Message::getKnowledgeBaseId)
                .eq(Message::getDeleted, false)
                .orderByAsc(Message::getCreatedAt)
                .orderByAsc(Message::getId));
    }

    @Override
    public List<Message> listRecentContextMessages(String conversationUid, Long userId, Long knowledgeBaseId, int limit) {
        List<Message> messages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationUid, conversationUid)
                .eq(Message::getUserId, userId)
                .eq(knowledgeBaseId != null, Message::getKnowledgeBaseId, knowledgeBaseId)
                .isNull(knowledgeBaseId == null, Message::getKnowledgeBaseId)
                .eq(Message::getDeleted, false)
                .orderByDesc(Message::getCreatedAt)
                .orderByDesc(Message::getId)
                .last("LIMIT " + limit));
        List<Message> ordered = new ArrayList<>(messages);
        Collections.reverse(ordered);
        return ordered;
    }

    private Message baseMessage(Long userId, Long knowledgeBaseId, String conversationUid, String role, String content) {
        LocalDateTime now = LocalDateTime.now();
        Message message = new Message();
        message.setMessageUid(UUID.randomUUID().toString());
        message.setConversationUid(conversationUid);
        message.setUserId(userId);
        message.setKnowledgeBaseId(knowledgeBaseId);
        message.setScopeType(knowledgeBaseId == null ? SCOPE_ENTERPRISE_ALL : SCOPE_KNOWLEDGE_BASE);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(now);
        message.setDeleted(false);
        return message;
    }
}
