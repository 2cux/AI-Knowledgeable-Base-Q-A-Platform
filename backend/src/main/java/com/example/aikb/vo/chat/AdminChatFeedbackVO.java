package com.example.aikb.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Admin chat feedback list item")
public class AdminChatFeedbackVO {

    @Schema(description = "Feedback ID", example = "1")
    private Long id;

    @Schema(description = "Chat record ID", example = "1")
    private Long chatRecordId;

    @Schema(description = "Feedback user ID", example = "1")
    private Long userId;

    @Schema(description = "Knowledge base ID", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "Conversation ID")
    private String conversationId;

    @Schema(description = "Message ID; null when the feedback table is chat-record based")
    private String messageId;

    @Schema(description = "Question")
    private String question;

    @Schema(description = "Answer preview")
    private String answerPreview;

    @Schema(description = "Feedback rating", example = "DISLIKE")
    private String rating;

    @Schema(description = "Feedback type", example = "DISLIKE")
    private String feedbackType;

    @Schema(description = "Feedback reason parsed from the stored comment prefix")
    private String reason;

    @Schema(description = "Feedback comment")
    private String comment;

    @Schema(description = "Handled status; null when unsupported by the current feedback table")
    private Boolean handled;

    @Schema(description = "Feedback created time")
    private LocalDateTime createdAt;
}
