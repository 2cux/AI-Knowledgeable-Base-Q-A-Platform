package com.example.aikb.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "问答请求参数")
public class ChatAskRequest {

    /** 提问限定在指定知识库内，服务层会校验该知识库是否属于当前登录用户。 */
    @NotNull(message = "knowledgeBaseId不能为空")
    @Positive(message = "knowledgeBaseId必须大于0")
    @Schema(description = "知识库ID", example = "1")
    private Long knowledgeBaseId;

    /** 用户输入的问题文本。 */
    @NotBlank(message = "question不能为空")
    @Size(max = 2000, message = "question不能超过2000个字符")
    @Schema(description = "用户问题", example = "如何重置管理员密码？")
    private String question;

    /** 检索返回的切片数量，不传则使用默认值。 */
    @Min(value = 1, message = "topK必须大于等于1")
    @Max(value = 20, message = "topK不能超过20")
    @Schema(description = "检索返回的切片数量", example = "5")
    private Integer topK;
}
