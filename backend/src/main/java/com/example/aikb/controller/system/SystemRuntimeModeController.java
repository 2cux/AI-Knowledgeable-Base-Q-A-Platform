package com.example.aikb.controller.system;

import com.example.aikb.common.Result;
import com.example.aikb.service.admin.AdminPermissionService;
import com.example.aikb.service.system.AiRuntimeStatusService;
import com.example.aikb.vo.system.RuntimeModeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统运行模式只读接口。
 *
 * <p>该接口用于管理员查看当前环境下 AI 相关能力的运行模式，不返回真实密钥或敏感请求头。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/runtime-mode")
public class SystemRuntimeModeController {

    private final AdminPermissionService adminPermissionService;
    private final AiRuntimeStatusService aiRuntimeStatusService;

    @GetMapping
    public Result<RuntimeModeVO> getRuntimeMode() {
        adminPermissionService.ensureAdmin("仅管理员可查看系统运行模式");
        return Result.success(aiRuntimeStatusService.getRuntimeMode());
    }
}
