package com.example.aikb.controller.task;

import com.example.aikb.common.Result;
import com.example.aikb.service.task.TaskRecordService;
import com.example.aikb.vo.task.TaskRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务查询接口，提供当前用户权限内的任务详情查询。
 */
@Validated
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "任务管理", description = "任务状态查询接口")
public class TaskRecordController {

    private final TaskRecordService taskRecordService;

    /**
     * 查询当前用户可访问的任务详情。
     */
    @Operation(summary = "查询任务详情", description = "根据任务ID查询当前用户可访问的文档处理任务状态")
    @GetMapping("/{id}")
    public Result<TaskRecordVO> getById(@PathVariable @Positive(message = "任务ID必须大于0") Long id) {
        return Result.success(taskRecordService.getById(id));
    }
}
