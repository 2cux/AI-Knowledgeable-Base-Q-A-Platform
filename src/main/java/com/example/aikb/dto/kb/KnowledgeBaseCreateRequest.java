package com.example.aikb.dto.kb;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识库创建请求参数。
 */
@Data
@Schema(description = "知识库创建请求参数")
public class KnowledgeBaseCreateRequest {

    /** 知识库名称。 */
    @Schema(description = "知识库名称，最多 128 个字符", example = "产品知识库")
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 128, message = "知识库名称不能超过128个字符")
    private String name;

    /** 知识库描述。 */
    @Schema(description = "知识库描述，最多 500 个字符", example = "用于管理产品文档和问答资料")
    @Size(max = 500, message = "知识库描述不能超过500个字符")
    private String description;
}
