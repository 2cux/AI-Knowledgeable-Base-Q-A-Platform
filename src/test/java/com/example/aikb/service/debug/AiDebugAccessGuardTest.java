package com.example.aikb.service.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.aikb.config.AppDebugAiTestProperties;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.admin.AdminPermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AiDebugAccessGuardTest {

    @Test
    void shouldRequireAdminInDevProfileWhenPropertyIsDisabled() {
        AppDebugAiTestProperties properties = new AppDebugAiTestProperties();
        properties.setEnabled(false);
        AdminPermissionService adminPermissionService = mock(AdminPermissionService.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        AiDebugAccessGuard guard = new AiDebugAccessGuard(properties, adminPermissionService, environment);

        guard.ensureAccessible();

        verify(adminPermissionService).ensureAdmin("仅管理员可访问 AI 调试接口");
    }

    @Test
    void shouldRejectWhenNotDevAndPropertyIsDisabled() {
        AppDebugAiTestProperties properties = new AppDebugAiTestProperties();
        properties.setEnabled(false);
        AdminPermissionService adminPermissionService = mock(AdminPermissionService.class);
        MockEnvironment environment = new MockEnvironment();

        AiDebugAccessGuard guard = new AiDebugAccessGuard(properties, adminPermissionService, environment);

        BusinessException exception = assertThrows(BusinessException.class, guard::ensureAccessible);

        assertEquals(40301, exception.getCode());
        assertEquals(403, exception.getHttpStatus());
    }

    @Test
    void shouldRequireAdminWhenExplicitlyEnabledOutsideDev() {
        AppDebugAiTestProperties properties = new AppDebugAiTestProperties();
        properties.setEnabled(true);
        AdminPermissionService adminPermissionService = mock(AdminPermissionService.class);
        MockEnvironment environment = new MockEnvironment();

        AiDebugAccessGuard guard = new AiDebugAccessGuard(properties, adminPermissionService, environment);

        guard.ensureAccessible();

        verify(adminPermissionService).ensureAdmin("仅管理员可访问 AI 调试接口");
    }
}
