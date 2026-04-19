package com.example.aikb.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 未命中问题列表项，用于管理端快速查看需要补充知识的问题。
 */
@Data
@Builder
@Schema(description = "未命中问题列表项")
public class MissedQuestionItemVO {

    @Schema(description = "问答记录ID", example = "1")
    private Long id;

    @Schema(description = "知识库ID", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "用户问题")
    private String question;

    @Schema(description = "命中的切片数量", example = "0")
    private Integer retrievedChunkCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
