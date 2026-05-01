package com.example.aikb.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aikb.common.PageResult;
import com.example.aikb.entity.Conversation;
import com.example.aikb.entity.Message;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.ConversationMapper;
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.chat.ConversationService;
import com.example.aikb.service.chat.MessageService;
import com.example.aikb.vo.chat.ConversationDetailVO;
import com.example.aikb.vo.chat.ConversationListItemVO;
import com.example.aikb.vo.chat.MessageVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private static final int TITLE_LENGTH = 30;
    private static final int ANSWER_PREVIEW_LENGTH = 120;

    private final ConversationMapper conversationMapper;
    private final MessageService messageService;
    private final CitationJsonCodec citationJsonCodec;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Conversation resolveForAsk(Long userId, Long knowledgeBaseId, String conversationId, String question) {
        if (conversationId == null || conversationId.isBlank()) {
            return create(userId, knowledgeBaseId, question);
        }
        Conversation conversation = getByUid(conversationId.trim());
        validateOwnerAndKnowledgeBase(conversation, userId, knowledgeBaseId);
        return conversation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void touchAfterAsk(String conversationUid, String question, String answer, int appendedMessageCount) {
        Conversation conversation = getByUid(conversationUid);
        conversation.setMessageCount((conversation.getMessageCount() == null ? 0 : conversation.getMessageCount())
                + appendedMessageCount);
        conversation.setLastQuestion(question);
        conversation.setLastAnswerPreview(preview(answer));
        conversation.setLastActiveAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
    }

    @Override
    public PageResult<ConversationListItemVO> pageCurrentUser(Long knowledgeBaseId, long pageNum, long pageSize) {
        Long userId = CurrentUser.getUserId();
        IPage<Conversation> result = conversationMapper.selectPage(Page.of(pageNum, pageSize),
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserId, userId)
                        .eq(knowledgeBaseId != null, Conversation::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(Conversation::getDeleted, false)
                        .orderByDesc(Conversation::getLastActiveAt)
                        .orderByDesc(Conversation::getId));
        List<ConversationListItemVO> list = result.getRecords().stream()
                .map(this::toListItemVO)
                .toList();
        return PageResult.<ConversationListItemVO>builder()
                .list(list)
                .total(result.getTotal())
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public ConversationDetailVO getCurrentUserDetail(String conversationId) {
        Long userId = CurrentUser.getUserId();
        Conversation conversation = getByUid(conversationId);
        if (!userId.equals(conversation.getUserId())) {
            throw new BusinessException(40300, "无权访问当前会话");
        }
        List<MessageVO> messages = messageService
                .listByConversation(conversation.getConversationUid(), userId, conversation.getKnowledgeBaseId())
                .stream()
                .map(this::toMessageVO)
                .toList();
        return ConversationDetailVO.builder()
                .conversationId(conversation.getConversationUid())
                .title(conversation.getTitle())
                .knowledgeBaseId(conversation.getKnowledgeBaseId())
                .createdAt(conversation.getCreatedAt())
                .lastActiveAt(conversation.getLastActiveAt())
                .messages(messages)
                .build();
    }

    private Conversation create(Long userId, Long knowledgeBaseId, String question) {
        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = new Conversation();
        conversation.setConversationUid(UUID.randomUUID().toString());
        conversation.setUserId(userId);
        conversation.setKnowledgeBaseId(knowledgeBaseId);
        conversation.setTitle(shorten(question, TITLE_LENGTH));
        conversation.setMessageCount(0);
        conversation.setLastActiveAt(now);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversation.setDeleted(false);
        conversationMapper.insert(conversation);
        return conversation;
    }

    private Conversation getByUid(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new BusinessException(40001, "conversationId不能为空");
        }
        Conversation conversation = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getConversationUid, conversationId.trim())
                .eq(Conversation::getDeleted, false)
                .last("LIMIT 1"));
        if (conversation == null) {
            throw new BusinessException(40400, "会话不存在");
        }
        return conversation;
    }

    private void validateOwnerAndKnowledgeBase(Conversation conversation, Long userId, Long knowledgeBaseId) {
        if (!userId.equals(conversation.getUserId())) {
            throw new BusinessException(40300, "无权访问当前会话");
        }
        if (!knowledgeBaseId.equals(conversation.getKnowledgeBaseId())) {
            throw new BusinessException(40001, "conversationId与knowledgeBaseId不一致");
        }
    }

    private ConversationListItemVO toListItemVO(Conversation conversation) {
        return ConversationListItemVO.builder()
                .conversationId(conversation.getConversationUid())
                .title(conversation.getTitle())
                .knowledgeBaseId(conversation.getKnowledgeBaseId())
                .messageCount(conversation.getMessageCount())
                .lastQuestion(conversation.getLastQuestion())
                .lastAnswerPreview(conversation.getLastAnswerPreview())
                .lastActiveAt(conversation.getLastActiveAt())
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    private MessageVO toMessageVO(Message message) {
        return MessageVO.builder()
                .messageId(message.getMessageUid())
                .role(message.getRole())
                .content(message.getContent())
                .citations(citationJsonCodec.deserialize(message.getCitations(), message.getId()))
                .chatRecordId(message.getChatRecordId())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private String preview(String answer) {
        return shorten(answer, ANSWER_PREVIEW_LENGTH);
    }

    private String shorten(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
