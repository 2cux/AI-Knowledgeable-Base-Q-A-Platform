package com.example.aikb.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Conversation detail")
public class ConversationDetailVO {

    private String conversationId;
    private String title;
    private Long knowledgeBaseId;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
    private List<MessageVO> messages;
}
