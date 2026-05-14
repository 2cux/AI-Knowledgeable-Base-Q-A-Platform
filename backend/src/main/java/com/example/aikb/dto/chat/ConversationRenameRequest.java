package com.example.aikb.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConversationRenameRequest {

    @Positive(message = "knowledgeBaseId必须大于0")
    private Long knowledgeBaseId;

    @NotBlank(message = "title不能为空")
    @Size(max = 100, message = "title不能超过100个字符")
    private String title;
}
