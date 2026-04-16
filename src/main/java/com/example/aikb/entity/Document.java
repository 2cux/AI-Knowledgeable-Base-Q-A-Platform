package com.example.aikb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文档实体，保存上传文档的元数据及其所属知识库关系。
 */
@Data
@TableName("document")
@Schema(description = "文档实体")
public class Document {

    /** 文档主键 ID。 */
    @Schema(description = "文档主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档所属知识库 ID。 */
    @Schema(description = "文档所属知识库 ID", example = "1")
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    /** 原始文件名称。 */
    @Schema(description = "原始文件名称", example = "产品手册.pdf")
    @TableField("file_name")
    private String fileName;

    /** 文件类型，例如 pdf、docx、txt。 */
    @Schema(description = "文件类型", example = "pdf")
    @TableField("file_type")
    private String fileType;

    /** 文件大小，单位为字节。 */
    @Schema(description = "文件大小，单位为字节", example = "204800")
    @TableField("file_size")
    private Long fileSize;

    /** 文件存储路径，MVP 阶段可保存占位路径。 */
    @Schema(description = "文件存储路径", example = "kb/1/产品手册.pdf")
    @TableField("storage_path")
    private String storagePath;

    /** 解析状态，例如 UPLOADED、PENDING、DONE、FAILED。 */
    @Schema(description = "解析状态", example = "UPLOADED")
    @TableField("parse_status")
    private String parseStatus;

    /** 上传用户 ID。 */
    @Schema(description = "上传用户 ID", example = "1")
    @TableField("created_by")
    private Long createdBy;

    /** 创建时间。 */
    @Schema(description = "创建时间")
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @Schema(description = "更新时间")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
