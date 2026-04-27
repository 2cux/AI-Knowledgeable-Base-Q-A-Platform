package com.example.aikb.vo.chat;

import com.example.aikb.service.chat.AnswerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Chat ask response")
public class ChatAskResponse {

    @Schema(description = "Conversation ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private String conversationId;

    @Schema(description = "Generated answer")
    private String answer;

    @Schema(description = "RAG answer status", example = "SUCCESS")
    private AnswerStatus answerStatus;

    @Schema(description = "Whether effective evidence chunks exist", example = "true")
    private Boolean matched;

    @Schema(description = "Effective chunk count. Legacy field kept for compatibility.", example = "3")
    private Integer retrievedChunkCount;

    @Schema(description = "Raw retrieved chunk count", example = "5")
    private Integer rawRetrievedChunkCount;

    @Schema(description = "Effective chunk count", example = "3")
    private Integer effectiveChunkCount;

    @Schema(description = "Minimum effective score", example = "0.2")
    private Double minEffectiveScore;

    @Schema(description = "Answer citations")
    private List<CitationVO> citations;
}
