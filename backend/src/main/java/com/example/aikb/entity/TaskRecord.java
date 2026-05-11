package com.example.aikb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 任务记录实体，用于记录文档处理等任务的基础状态和执行时间。
 */
@Data
@TableName("task_record")
@Schema(description = "任务记录实体")
public class TaskRecord {

    /** 任务主键 ID。 */
    @Schema(description = "任务主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务类型，例如 DOCUMENT_PROCESS。 */
    @Schema(description = "任务类型", example = "DOCUMENT_PROCESS")
    @TableField("task_type")
    private String taskType;

    /** 业务类型，例如 DOCUMENT。 */
    @Schema(description = "业务类型", example = "DOCUMENT")
    @TableField("biz_type")
    private String bizType;

    /** 业务数据 ID，例如文档 ID。 */
    @Schema(description = "业务数据 ID", example = "1")
    @TableField("biz_id")
    private Long bizId;

    /** 任务状态，例如 PENDING、PROCESSING、SUCCESS、FAILED。 */
    @Schema(description = "任务状态", example = "PENDING")
    private String status;

    /** 任务失败时的错误信息。 */
    @Schema(description = "任务失败时的错误信息")
    @TableField("error_message")
    private String errorMessage;

    /** 重试次数。 */
    @Schema(description = "重试次数", example = "0")
    @TableField("retry_count")
    private Integer retryCount;

    /** 任务创建人 ID。 */
    @Schema(description = "任务创建人 ID", example = "1")
    @TableField("created_by")
    private Long createdBy;

    /** 任务开始执行时间。 */
    @Schema(description = "任务开始执行时间")
    @TableField("started_at")
    private LocalDateTime startedAt;

    /** 任务结束执行时间。 */
    @Schema(description = "任务结束执行时间")
    @TableField("finished_at")
    private LocalDateTime finishedAt;

    /** 创建时间。 */
    @Schema(description = "创建时间")
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @Schema(description = "更新时间")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
