package com.example.aikb.controller.admin;

import com.example.aikb.common.PageResult;
import com.example.aikb.common.Result;
import com.example.aikb.service.admin.AdminDashboardService;
import com.example.aikb.service.chat.AdminChatRecordQueryService;
import com.example.aikb.vo.admin.AdminDashboardVO;
import com.example.aikb.vo.chat.AdminChatFeedbackVO;
import com.example.aikb.vo.chat.AdminChatRecordDetailVO;
import com.example.aikb.vo.chat.AdminChatRecordListItemVO;
import com.example.aikb.vo.chat.AdminMissedQuestionVO;
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

@Validated
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminMvpController {

    private final AdminDashboardService adminDashboardService;
    private final AdminChatRecordQueryService adminChatRecordQueryService;

    @GetMapping("/dashboard")
    public Result<AdminDashboardVO> dashboard() {
        return Result.success(adminDashboardService.getDashboard());
    }

    @GetMapping("/chat-records")
    public Result<PageResult<AdminChatRecordListItemVO>> chatRecords(
            @RequestParam(required = false) @Positive Long knowledgeBaseId,
            @RequestParam(required = false) Boolean matched,
            @RequestParam(required = false) String answerStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize) {
        return Result.success(adminChatRecordQueryService.page(knowledgeBaseId, matched, answerStatus, keyword, page,
                pageSize));
    }

    @GetMapping("/chat-records/{id}")
    public Result<AdminChatRecordDetailVO> chatRecordDetail(@PathVariable @Positive Long id) {
        return Result.success(adminChatRecordQueryService.getById(id));
    }

    @GetMapping("/feedback")
    public Result<PageResult<AdminChatFeedbackVO>> feedback(
            @RequestParam(required = false) @Positive Long knowledgeBaseId,
            @RequestParam(required = false) String feedbackType,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Boolean handled,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize) {
        return Result.success(adminChatRecordQueryService.pageFeedback(knowledgeBaseId, feedbackType, reason, null,
                null, page, pageSize));
    }

    @GetMapping("/unmatched-questions")
    public Result<PageResult<AdminMissedQuestionVO>> unmatchedQuestions(
            @RequestParam(required = false) @Positive Long knowledgeBaseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize) {
        return Result.success(adminChatRecordQueryService.pageUnmatchedQuestions(knowledgeBaseId, keyword, page,
                pageSize));
    }
}
