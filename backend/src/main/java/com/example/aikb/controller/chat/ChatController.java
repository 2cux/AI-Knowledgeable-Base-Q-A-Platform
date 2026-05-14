package com.example.aikb.controller.chat;

import com.example.aikb.common.PageResult;
import com.example.aikb.common.Result;
import com.example.aikb.dto.chat.ChatAskRequest;
import com.example.aikb.dto.chat.ConversationPinRequest;
import com.example.aikb.dto.chat.ConversationRenameRequest;
import com.example.aikb.service.chat.ChatService;
import com.example.aikb.service.chat.ConversationService;
import com.example.aikb.vo.chat.ChatAskResponse;
import com.example.aikb.vo.chat.ConversationDetailVO;
import com.example.aikb.vo.chat.ConversationListItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "问答模块", description = "最小RAG问答链路")
public class ChatController {

    private final ChatService chatService;
    private final ConversationService conversationService;

    @Operation(summary = "发起提问", description = "检索相关切片，生成答案，并保存问答记录")
    @PostMapping("/ask")
    public Result<ChatAskResponse> ask(@Valid @RequestBody ChatAskRequest request) {
        return Result.success(chatService.ask(request));
    }

    @Operation(summary = "全企业知识库问答", description = "自动从已发布且已完成 embedding 的企业知识库中检索并生成答案")
    @PostMapping("/ask-global")
    public Result<ChatAskResponse> askGlobal(@Valid @RequestBody ChatAskRequest request) {
        return Result.success(chatService.askGlobal(request));
    }

    @Operation(summary = "分页查询会话列表", description = "分页查询当前登录用户自己的会话")
    @GetMapping("/conversations")
    public Result<PageResult<ConversationListItemVO>> conversations(
            @RequestParam(required = false) @Positive(message = "knowledgeBaseId必须大于0") Long knowledgeBaseId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page不能小于1") long page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size不能小于1")
            @Max(value = 100, message = "size不能大于100") long size) {
        return Result.success(conversationService.pageCurrentUser(knowledgeBaseId, page, size));
    }

    @Operation(summary = "查询会话详情", description = "查询当前登录用户自己的指定会话及消息")
    @GetMapping("/conversations/{conversationId}")
    public Result<ConversationDetailVO> conversationDetail(@PathVariable String conversationId) {
        return Result.success(conversationService.getCurrentUserDetail(conversationId));
    }

    @Operation(summary = "重命名会话", description = "重命名当前登录用户自己的指定会话")
    @PatchMapping("/conversations/{conversationId}/title")
    public Result<ConversationListItemVO> renameConversation(
            @PathVariable String conversationId,
            @Valid @RequestBody ConversationRenameRequest request) {
        return Result.success(conversationService.renameCurrentUserConversation(
                conversationId, request.getKnowledgeBaseId(), request.getTitle()));
    }

    @Operation(summary = "置顶或取消置顶会话", description = "置顶或取消置顶当前登录用户自己的指定会话")
    @PatchMapping("/conversations/{conversationId}/pin")
    public Result<ConversationListItemVO> pinConversation(
            @PathVariable String conversationId,
            @Valid @RequestBody ConversationPinRequest request) {
        return Result.success(conversationService.pinCurrentUserConversation(
                conversationId, request.getKnowledgeBaseId(), request.getPinned()));
    }

    @Operation(summary = "删除会话", description = "软删除当前登录用户自己的指定会话")
    @DeleteMapping("/conversations/{conversationId}")
    public Result<Void> deleteConversation(
            @PathVariable String conversationId,
            @RequestParam(required = false) @Positive(message = "knowledgeBaseId必须大于0") Long knowledgeBaseId) {
        conversationService.deleteCurrentUserConversation(conversationId, knowledgeBaseId);
        return Result.success();
    }
}
