package com.example.aikb.controller.chat;

import com.example.aikb.common.Result;
import com.example.aikb.dto.chat.ChatFeedbackRequest;
import com.example.aikb.service.chat.ChatFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 问答反馈接口，负责接收用户对某条问答记录的反馈。
 */
@Validated
@RestController
@RequestMapping("/api/chat/records")
@RequiredArgsConstructor
@Tag(name = "问答反馈", description = "对历史问答记录提交点赞或点踩反馈")
public class ChatFeedbackController {

    private final ChatFeedbackService chatFeedbackService;

    @Operation(summary = "提交问答反馈", description = "当前登录用户对自己的问答记录提交点赞或点踩反馈")
    @PostMapping("/{id}/feedback")
    public Result<Void> submit(
            @PathVariable @Positive(message = "问答记录ID必须大于0") Long id,
            @Valid @RequestBody ChatFeedbackRequest request) {
        chatFeedbackService.submit(id, request);
        return Result.success(null, "反馈提交成功");
    }
}
