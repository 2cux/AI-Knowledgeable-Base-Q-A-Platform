package com.example.aikb.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 问答记录详情，返回完整问题和答案。
 */
@Data
@Builder
@Schema(description = "问答记录详情")
public class ChatRecordDetailVO {

    @Schema(description = "问答记录ID", example = "1")
    private Long id;

    @Schema(description = "知识库ID", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "用户问题")
    private String question;

    @Schema(description = "完整答案")
    private String answer;

    @Schema(description = "是否命中切片", example = "true")
    private Boolean matched;

    @Schema(description = "命中的切片数量", example = "3")
    private Integer retrievedChunkCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
