package com.example.aikb.controller.admin;

import com.example.aikb.common.PageResult;
import com.example.aikb.common.Result;
import com.example.aikb.service.chat.AdminChatRecordQueryService;
import com.example.aikb.vo.chat.AdminChatRecordDetailVO;
import com.example.aikb.vo.chat.AdminChatRecordListItemVO;
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
 * 管理端问答日志查询接口，用于运营分析和问题排查。
 */
@Validated
@RestController
@RequestMapping("/api/admin/chat/records")
@RequiredArgsConstructor
@Tag(name = "管理端问答日志", description = "管理员查看系统问答记录")
public class AdminChatRecordController {

    private final AdminChatRecordQueryService adminChatRecordQueryService;

    @Operation(summary = "分页查询问答日志", description = "管理端按知识库和命中状态过滤问答记录")
    @GetMapping
    public Result<PageResult<AdminChatRecordListItemVO>> page(
            @RequestParam(required = false) @Positive(message = "knowledgeBaseId必须大于0") Long knowledgeBaseId,
            @RequestParam(required = false) Boolean matched,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNum不能小于1") long pageNum,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "pageSize不能小于1")
            @Max(value = 100, message = "pageSize不能大于100") long pageSize) {
        return Result.success(adminChatRecordQueryService.page(knowledgeBaseId, matched, pageNum, pageSize));
    }

    @Operation(summary = "查询问答日志详情", description = "管理端查看单条问答记录完整内容")
    @GetMapping("/{id}")
    public Result<AdminChatRecordDetailVO> getById(
            @PathVariable @Positive(message = "问答记录ID必须大于0") Long id) {
        return Result.success(adminChatRecordQueryService.getById(id));
    }
}
