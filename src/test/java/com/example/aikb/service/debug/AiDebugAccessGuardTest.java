package com.example.aikb.service.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.aikb.config.AppDebugApiProperties;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.admin.AdminPermissionService;
import org.junit.jupiter.api.Test;

class AiDebugAccessGuardTest {

    @Test
    void shouldRejectBeforeAdminCheckWhenDebugApiDisabled() {
        AppDebugApiProperties properties = new AppDebugApiProperties();
        properties.setEnabled(false);
        AdminPermissionService adminPermissionService = mock(AdminPermissionService.class);
        AiDebugAccessGuard guard = new AiDebugAccessGuard(properties, adminPermissionService);

        BusinessException exception = assertThrows(BusinessException.class, guard::ensureAccessible);

        assertEquals(40301, exception.getCode());
        assertEquals(403, exception.getHttpStatus());
        verify(adminPermissionService, never()).ensureAdmin(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldRequireAdminWhenDebugApiEnabled() {
        AppDebugApiProperties properties = new AppDebugApiProperties();
        properties.setEnabled(true);
        AdminPermissionService adminPermissionService = mock(AdminPermissionService.class);
        AiDebugAccessGuard guard = new AiDebugAccessGuard(properties, adminPermissionService);

        guard.ensureAccessible();

        verify(adminPermissionService).ensureAdmin("仅管理员可访问 debug 接口");
    }

    @Test
    void shouldRejectWhenDebugApiEnabledButUserNotLoggedIn() {
        AppDebugApiProperties properties = new AppDebugApiProperties();
        properties.setEnabled(true);
        AdminPermissionService adminPermissionService = mock(AdminPermissionService.class);
        doThrow(new BusinessException(40100, "用户未登录"))
                .when(adminPermissionService).ensureAdmin(org.mockito.ArgumentMatchers.anyString());
        AiDebugAccessGuard guard = new AiDebugAccessGuard(properties, adminPermissionService);

        BusinessException exception = assertThrows(BusinessException.class, guard::ensureAccessible);

        assertEquals(40100, exception.getCode());
        assertEquals(401, exception.getHttpStatus());
    }

    @Test
    void shouldRejectWhenDebugApiEnabledButUserIsNotAdmin() {
        AppDebugApiProperties properties = new AppDebugApiProperties();
        properties.setEnabled(true);
        AdminPermissionService adminPermissionService = mock(AdminPermissionService.class);
        doThrow(new BusinessException(40300, "仅管理员可访问 debug 接口"))
                .when(adminPermissionService).ensureAdmin(org.mockito.ArgumentMatchers.anyString());
        AiDebugAccessGuard guard = new AiDebugAccessGuard(properties, adminPermissionService);

        BusinessException exception = assertThrows(BusinessException.class, guard::ensureAccessible);

        assertEquals(40300, exception.getCode());
        assertEquals(403, exception.getHttpStatus());
    }
}
