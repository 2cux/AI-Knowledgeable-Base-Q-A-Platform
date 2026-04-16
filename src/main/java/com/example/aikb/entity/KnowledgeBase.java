package com.example.aikb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 知识库实体，保存知识库基础信息和归属用户。
 */
@Data
@TableName("knowledge_base")
@Schema(description = "知识库实体")
public class KnowledgeBase {

    /** 知识库主键 ID。 */
    @Schema(description = "知识库主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 知识库名称。 */
    @Schema(description = "知识库名称", example = "产品知识库")
    private String name;

    /** 知识库描述。 */
    @Schema(description = "知识库描述", example = "用于管理产品文档和问答资料")
    private String description;

    /** 知识库所属用户 ID。 */
    @Schema(description = "知识库所属用户 ID", example = "1")
    @TableField("owner_id")
    private Long ownerId;

    /** 知识库状态。 */
    @Schema(description = "知识库状态", example = "1")
    private Integer status;

    /** 创建时间。 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
