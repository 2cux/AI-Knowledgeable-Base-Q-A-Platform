package com.example.aikb.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Conversation message")
public class MessageVO {

    private String messageId;
    private String role;
    private String content;
    private List<CitationVO> citations;
    private Long chatRecordId;
    private Long feedbackId;
    private String feedbackType;
    private String feedbackComment;
    private LocalDateTime feedbackCreatedAt;
    private LocalDateTime createdAt;
}
