package com.example.aikb.vo.kb;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 知识库展示信息。
 */
@Data
@Builder
@Schema(description = "知识库展示信息")
public class KnowledgeBaseVO {

    /** 知识库 ID。 */
    @Schema(description = "知识库 ID", example = "1")
    private Long id;

    /** 知识库名称。 */
    @Schema(description = "知识库名称", example = "产品知识库")
    private String name;

    /** 知识库描述。 */
    @Schema(description = "知识库描述", example = "用于管理产品文档和问答资料")
    private String description;

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
