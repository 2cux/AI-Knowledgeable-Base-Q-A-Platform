package com.example.aikb.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin basic chat statistics")
public class AdminChatStatsVO {

    @Schema(description = "Total chat count", example = "100")
    private Long totalChatCount;

    @Schema(description = "Matched chat count", example = "80")
    private Long matchedCount;

    @Schema(description = "Missed chat count", example = "20")
    private Long missedCount;

    @Schema(description = "Match rate", example = "0.8000")
    private BigDecimal matchRate;

    @Schema(description = "Total feedback count", example = "15")
    private Long feedbackCount;

    @Schema(description = "LIKE feedback count", example = "10")
    private Long likeCount;

    @Schema(description = "DISLIKE feedback count", example = "5")
    private Long dislikeCount;
}
