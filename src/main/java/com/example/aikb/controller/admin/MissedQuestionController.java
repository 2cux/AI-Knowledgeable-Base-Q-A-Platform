package com.example.aikb.controller.admin;

import com.example.aikb.common.PageResult;
import com.example.aikb.common.Result;
import com.example.aikb.service.chat.MissedQuestionQueryService;
import com.example.aikb.vo.chat.MissedQuestionItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端未命中问题查询接口，用于发现需要补充知识的问题。
 */
@Validated
@RestController
@RequestMapping("/api/admin/chat/missed")
@RequiredArgsConstructor
@Tag(name = "未命中问题管理", description = "管理端查看问答过程中未命中的问题")
public class MissedQuestionController {

    private final MissedQuestionQueryService missedQuestionQueryService;

    @Operation(summary = "分页查询未命中问题", description = "基于chat_record中matched=false的记录查询未命中问题")
    @GetMapping
    public Result<PageResult<MissedQuestionItemVO>> page(
            @RequestParam(required = false) @Positive(message = "knowledgeBaseId必须大于0") Long knowledgeBaseId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "pageNum不能小于1") long pageNum,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "pageSize不能小于1")
            @Max(value = 100, message = "pageSize不能大于100") long pageSize) {
        return Result.success(missedQuestionQueryService.page(knowledgeBaseId, pageNum, pageSize));
    }
}
