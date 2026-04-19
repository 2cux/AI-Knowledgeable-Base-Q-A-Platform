package com.example.aikb.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 管理端问答日志列表项，只返回列表展示需要的轻量字段。
 */
@Data
@Builder
@Schema(description = "管理端问答日志列表项")
public class AdminChatRecordListItemVO {

    @Schema(description = "问答记录ID", example = "1")
    private Long id;

    @Schema(description = "知识库ID", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "用户问题")
    private String question;

    @Schema(description = "答案摘要")
    private String answerPreview;

    @Schema(description = "是否命中切片", example = "true")
    private Boolean matched;

    @Schema(description = "命中的切片数量", example = "3")
    private Integer retrievedChunkCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
