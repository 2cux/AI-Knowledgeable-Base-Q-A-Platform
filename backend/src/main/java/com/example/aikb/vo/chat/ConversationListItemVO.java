package com.example.aikb.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Conversation list item")
public class ConversationListItemVO {

    private String conversationId;
    private String title;
    private Long knowledgeBaseId;

    private String scopeType;
    private Integer messageCount;
    private String lastQuestion;
    private String lastAnswerPreview;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
    private Boolean pinned;
    private LocalDateTime pinnedAt;
}
