package com.example.aikb.dto.document;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文档向量化请求参数。
 */
@Data
@Schema(description = "文档向量化请求参数")
public class DocumentEmbeddingRequest {

    /** 向量化模型名称，不传则使用默认占位模型。 */
    @Schema(description = "向量化模型名称，不传则使用默认占位模型", example = "mock-embedding-v1")
    @Size(max = 128, message = "embeddingModel不能超过128个字符")
    private String embeddingModel;
}
