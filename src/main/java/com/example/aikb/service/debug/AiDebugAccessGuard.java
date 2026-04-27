package com.example.aikb.service.debug;

import com.example.aikb.config.AppDebugAiTestProperties;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.admin.AdminPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

/**
 * AI 调试接口访问守卫。
 *
 * <p>该守卫用于统一收口 /debug/ai/** 访问规则：仅用于本地开发/联调，生产环境不应开放。</p>
 */
@Service
@RequiredArgsConstructor
public class AiDebugAccessGuard {

    private final AppDebugAiTestProperties properties;
    private final AdminPermissionService adminPermissionService;
    private final Environment environment;

    /**
     * 校验当前请求是否允许访问 AI 调试接口。
     */
    public void ensureAccessible() {
        if (!isDebugEnabled()) {
            throw new BusinessException(40301,
                    "AI 调试接口未开启，仅允许 dev 环境本地联调，或显式开启 app.debug.ai-test.enabled 后访问");
        }
        adminPermissionService.ensureAdmin("仅管理员可访问 AI 调试接口");
    }

    /**
     * 当前环境是否允许启用 AI 调试接口。
     */
    public boolean isDebugEnabled() {
        return properties.isEnabled() || isDevProfile();
    }

    private boolean isDevProfile() {
        return environment.acceptsProfiles(Profiles.of("dev"));
    }
}
