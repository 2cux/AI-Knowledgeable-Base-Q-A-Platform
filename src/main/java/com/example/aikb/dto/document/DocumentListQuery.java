package com.example.aikb.dto.document;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 文档分页查询参数，支持按知识库筛选。
 */
@Data
@Schema(description = "文档分页查询参数")
public class DocumentListQuery {

    /** 当前页码，从 1 开始。 */
    @Schema(description = "当前页码，从 1 开始", example = "1")
    @Min(value = 1, message = "pageNum不能小于1")
    private long pageNum = 1;

    /** 每页数据条数。 */
    @Schema(description = "每页数据条数，最大 100", example = "10")
    @Min(value = 1, message = "pageSize不能小于1")
    @Max(value = 100, message = "pageSize不能大于100")
    private long pageSize = 10;

    /** 知识库 ID，为空时查询当前用户全部文档。 */
    @Schema(description = "知识库 ID，为空时查询当前用户全部文档", example = "1")
    @Positive(message = "知识库ID必须大于0")
    private Long knowledgeBaseId;
}
