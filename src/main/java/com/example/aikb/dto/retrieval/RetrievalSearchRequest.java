package com.example.aikb.dto.retrieval;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 检索请求参数，只承载检索所需字段，不包含问答生成相关参数。
 */
@Data
@Schema(description = "检索请求参数")
public class RetrievalSearchRequest {

    /** 检索查询文本，真实检索主入口字段。 */
    @Size(max = 2000, message = "query不能超过2000个字符")
    @Schema(description = "检索查询文本", example = "如何重置管理员密码？")
    private String query;

    /** 兼容旧问答调试调用，服务层会优先使用 query。 */
    @Size(max = 2000, message = "question不能超过2000个字符")
    @Schema(description = "兼容旧调用的问题文本", example = "如何重置管理员密码？")
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
