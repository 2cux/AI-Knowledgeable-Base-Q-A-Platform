package com.example.aikb.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 问答反馈请求参数。
 */
@Data
@Schema(description = "问答反馈请求参数")
public class ChatFeedbackRequest {

    /** 反馈类型，MVP阶段只支持 LIKE 和 DISLIKE。 */
    @NotBlank(message = "feedbackType不能为空")
    @Schema(description = "反馈类型：LIKE 或 DISLIKE", example = "LIKE")
    private String feedbackType;

    /** 用户补充说明，可为空。 */
    @Size(max = 500, message = "comment不能超过500个字符")
    @Schema(description = "反馈备注", example = "回答很准确")
    private String comment;
}
