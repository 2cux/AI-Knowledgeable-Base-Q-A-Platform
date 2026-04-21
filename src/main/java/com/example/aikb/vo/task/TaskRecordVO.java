package com.example.aikb.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 任务记录展示对象，用于查询文档处理任务的执行状态。
 */
@Data
@Builder
@Schema(description = "任务记录展示对象")
public class TaskRecordVO {

    /** 任务 ID。 */
    @Schema(description = "任务 ID", example = "1")
    private Long id;

    /** 文档 ID。 */
    @Schema(description = "文档 ID", example = "1")
    private Long documentId;

    /** 任务类型。 */
    @Schema(description = "任务类型", example = "DOCUMENT_PROCESS")
    private String taskType;

    /** 任务状态。 */
    @Schema(description = "任务状态", example = "SUCCESS")
    private String status;

    /** 失败错误信息。 */
    @Schema(description = "失败错误信息")
    private String errorMessage;

    /** 任务开始时间。 */
    @Schema(description = "任务开始时间")
    private LocalDateTime startedAt;

    /** 任务结束时间。 */
    @Schema(description = "任务结束时间")
    private LocalDateTime finishedAt;

    /** 任务创建人 ID。 */
    @Schema(description = "任务创建人 ID", example = "1")
    private Long createdBy;

    /** 创建时间。 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
