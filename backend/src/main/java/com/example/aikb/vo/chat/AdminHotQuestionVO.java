package com.example.aikb.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin hot question item")
public class AdminHotQuestionVO {

    @Schema(description = "Question")
    private String question;

    @Schema(description = "Asked count", example = "12")
    private Long count;

    @Schema(description = "Latest asked time")
    private LocalDateTime latestAskedAt;
}
