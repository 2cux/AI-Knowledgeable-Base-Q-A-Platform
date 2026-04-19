package com.example.aikb.controller.chat;

import com.example.aikb.common.Result;
import com.example.aikb.service.chat.ChatRecordQueryService;
import com.example.aikb.vo.chat.ChatRecordDetailVO;
import com.example.aikb.vo.chat.ChatRecordPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 历史问答记录接口，负责接收查询参数并委托服务层处理。
 */
@Validated
@RestController
@RequestMapping("/api/chat/records")
@RequiredArgsConstructor
@Tag(name = "历史问答记录", description = "问答记录分页查询和详情查询")
public class ChatRecordController {

    private final ChatRecordQueryService chatRecordQueryService;

    @Operation(summary = "分页查询问答记录", description = "分页查询当前登录用户自己的历史问答记录")
    @GetMapping
    public Result<ChatRecordPageResponse> page(
            @RequestParam(required = false) @Positive(message = "knowledgeBaseId必须大于0") Long knowledgeBaseId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNum不能小于1") long pageNum,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "pageSize不能小于1")
            @Max(value = 100, message = "pageSize不能大于100") long pageSize) {
        return Result.success(chatRecordQueryService.page(knowledgeBaseId, pageNum, pageSize));
    }

    @Operation(summary = "查询问答记录详情", description = "查询当前登录用户自己的指定问答记录详情")
    @GetMapping("/{id}")
    public Result<ChatRecordDetailVO> getById(@PathVariable @Positive(message = "问答记录ID必须大于0") Long id) {
        return Result.success(chatRecordQueryService.getById(id));
    }
}
