package com.example.aikb.dto.kb;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识库修改请求参数，仅允许修改普通用户可维护的基础信息。
 */
@Data
@Schema(description = "知识库修改请求参数")
public class KnowledgeBaseUpdateRequest {

    /** 知识库名称。 */
    @Schema(description = "知识库名称，最大 128 个字符", example = "新的知识库名称")
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 128, message = "知识库名称不能超过128个字符")
    private String name;

    /** 知识库描述。 */
    @Schema(description = "知识库描述，最大 500 个字符", example = "新的知识库描述")
    @Size(max = 500, message = "知识库描述不能超过500个字符")
    private String description;
}
