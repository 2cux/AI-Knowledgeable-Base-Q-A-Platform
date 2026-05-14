package com.example.aikb.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "问答反馈请求参数")
public class ChatFeedbackRequest {

    @NotBlank(message = "feedbackType不能为空")
    @Schema(description = "反馈类型：LIKE 或 DISLIKE", example = "LIKE")
    private String feedbackType;

    @Size(max = 600, message = "comment不能超过600个字符")
    @Schema(description = "反馈备注，前端会用前缀保存原因，用户补充说明最多500字", example = "[ANSWER_ACCURATE] 回答很准确")
    private String comment;
}
