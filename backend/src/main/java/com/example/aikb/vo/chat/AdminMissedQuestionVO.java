package com.example.aikb.vo.chat;

import com.example.aikb.service.chat.AnswerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Admin missed question list item")
public class AdminMissedQuestionVO {

    @Schema(description = "Chat record ID", example = "1")
    private Long id;

    @Schema(description = "Question user ID", example = "1")
    private Long userId;

    @Schema(description = "Knowledge base ID", example = "1")
    private Long knowledgeBaseId;

    private String scopeType;

    @Schema(description = "Conversation ID")
    private String conversationId;

    @Schema(description = "Question")
    private String question;

    @Schema(description = "Answer preview")
    private String answerPreview;

    @Schema(description = "RAG answer status", example = "NO_HIT")
    private AnswerStatus answerStatus;

    @Schema(description = "Whether effective evidence chunks exist", example = "false")
    private Boolean matched;

    @Schema(description = "Effective chunk count", example = "0")
    private Integer retrievedChunkCount;

    @Schema(description = "Raw retrieved chunk count before effective filtering", example = "0")
    private Integer rawRetrievedChunkCount;

    @Schema(description = "Requested retrieval topK", example = "5")
    private Integer topK;

    @Schema(description = "Created time")
    private LocalDateTime createdAt;
}
