package com.example.aikb.security;

import com.example.aikb.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具类，用于从安全上下文中获取认证用户信息。
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 当前登录用户 ID
     */
    public static Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(40100, "用户未登录");
        }
        return loginUser.getUserId();
    }
}
