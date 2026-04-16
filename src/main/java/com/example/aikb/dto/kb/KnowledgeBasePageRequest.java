package com.example.aikb.dto.kb;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 知识库分页查询请求参数。
 */
@Data
@Schema(description = "知识库分页查询请求参数")
public class KnowledgeBasePageRequest {

    /** 当前页码，从 1 开始。 */
    @Schema(description = "当前页码，从 1 开始", example = "1")
    @Min(value = 1, message = "pageNum不能小于1")
    private long pageNum = 1;

    /** 每页数据条数。 */
    @Schema(description = "每页数据条数，最大 100", example = "10")
    @Min(value = 1, message = "pageSize不能小于1")
    @Max(value = 100, message = "pageSize不能大于100")
    private long pageSize = 10;
}
