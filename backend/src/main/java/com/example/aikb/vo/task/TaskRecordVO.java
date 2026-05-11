package com.example.aikb.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 任务记录响应对象。
 */
@Data
@Builder
@Schema(description = "任务记录响应对象")
public class TaskRecordVO {

    @Schema(description = "任务ID", example = "1")
    private Long id;

    @Schema(description = "任务ID", example = "1")
    private Long taskId;

    @Schema(description = "文档ID", example = "1")
    private Long documentId;

    @Schema(description = "任务类型", example = "DOCUMENT_PROCESS")
    private String taskType;

    @Schema(description = "任务状态", example = "SUCCESS")
    private String status;

    @Schema(description = "失败信息")
    private String errorMessage;

    @Schema(description = "任务开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "任务结束时间")
    private LocalDateTime finishedAt;

    @Schema(description = "任务创建人用户ID", example = "1")
    private Long createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
