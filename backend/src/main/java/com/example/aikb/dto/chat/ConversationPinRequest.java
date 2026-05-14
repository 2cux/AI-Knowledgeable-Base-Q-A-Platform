package com.example.aikb.dto.chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ConversationPinRequest {

    @Positive(message = "knowledgeBaseId必须大于0")
    private Long knowledgeBaseId;

    @NotNull(message = "pinned不能为空")
    private Boolean pinned;
}
