package com.example.aikb.dto.retrieval;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 检索请求参数，只承载检索所需字段，不包含 QA 生成相关参数。
 */
@Data
@Schema(description = "检索请求参数")
public class RetrievalSearchRequest {

    /** 用户输入的问题，用于生成查询向量。 */
    @NotBlank(message = "question不能为空")
    @Size(max = 2000, message = "question不能超过2000个字符")
    @Schema(description = "用户问题", example = "如何重置管理员密码？")
    private String question;

    /** 检索范围限定在指定知识库，服务层会校验当前用户是否拥有该知识库。 */
    @NotNull(message = "knowledgeBaseId不能为空")
    @Positive(message = "knowledgeBaseId必须大于0")
    @Schema(description = "知识库ID", example = "1")
    private Long knowledgeBaseId;

    /** 返回的 chunk 数量。MVP 阶段限制上限，避免一次检索扫描过多数据。 */
    @Min(value = 1, message = "topK必须大于等于1")
    @Max(value = 20, message = "topK不能超过20")
    @Schema(description = "返回最相关chunk数量，不传默认5", example = "5")
    private Integer topK;
}
