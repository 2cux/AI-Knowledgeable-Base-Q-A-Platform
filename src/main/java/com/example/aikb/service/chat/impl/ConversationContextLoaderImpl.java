package com.example.aikb.service.chat.impl;

import com.example.aikb.entity.Message;
import com.example.aikb.service.chat.ConversationContextLoader;
import com.example.aikb.service.chat.MessageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversationContextLoaderImpl implements ConversationContextLoader {

    private static final int MAX_CONTEXT_MESSAGES = 10;
    private static final int MAX_CONTEXT_TEXT_LENGTH = 4000;
    private static final int MAX_MESSAGE_TEXT_LENGTH = 600;

    private final MessageService messageService;

    @Override
    public String load(Long userId, Long knowledgeBaseId, String conversationId) {
        List<Message> messages = messageService.listRecentContextMessages(conversationId, userId, knowledgeBaseId,
                MAX_CONTEXT_MESSAGES);
        if (messages.isEmpty()) {
            return "";
        }
        StringBuilder context = new StringBuilder("最近会话上下文：\n");
        for (Message message : messages) {
            String role = MessageServiceImpl.ROLE_USER.equals(message.getRole()) ? "用户" : "助手";
            appendWithinLimit(context, role + "：" + shorten(message.getContent(), MAX_MESSAGE_TEXT_LENGTH) + "\n");
            if (context.length() >= MAX_CONTEXT_TEXT_LENGTH) {
                break;
            }
        }
        return context.toString();
    }

    private void appendWithinLimit(StringBuilder context, String line) {
        int remaining = MAX_CONTEXT_TEXT_LENGTH - context.length();
        if (remaining <= 0) {
            return;
        }
        if (line.length() <= remaining) {
            context.append(line);
            return;
        }
        context.append(line, 0, remaining);
    }

    private String shorten(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
