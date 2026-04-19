package com.example.aikb.controller.chat;

import com.example.aikb.common.Result;
import com.example.aikb.dto.chat.ChatAskRequest;
import com.example.aikb.service.chat.ChatService;
import com.example.aikb.vo.chat.ChatAskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "问答模块", description = "最小RAG问答链路")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "发起提问", description = "检索相关切片，生成答案，并保存问答记录")
    @PostMapping("/ask")
    public Result<ChatAskResponse> ask(@Valid @RequestBody ChatAskRequest request) {
        return Result.success(chatService.ask(request));
    }
}
