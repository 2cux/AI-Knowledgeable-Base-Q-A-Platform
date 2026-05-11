package com.example.aikb.service.debug;

import com.example.aikb.config.AppDebugApiProperties;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.admin.AdminPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Central access guard for debug/test/internal endpoints.
 */
@Service
@RequiredArgsConstructor
public class AiDebugAccessGuard {

    private final AppDebugApiProperties properties;
    private final AdminPermissionService adminPermissionService;

    /**
     * Checks whether the current request may access debug/test/internal endpoints.
     */
    public void ensureAccessible() {
        if (!isDebugEnabled()) {
            throw new BusinessException(40301,
                    "debug 接口未开启，请在受控环境显式开启 app.debug-api.enabled 后访问");
        }
        adminPermissionService.ensureAdmin("仅管理员可访问 debug 接口");
    }

    /**
     * Whether the global debug API switch is enabled.
     */
    public boolean isDebugEnabled() {
        return properties.isEnabled();
    }
}
