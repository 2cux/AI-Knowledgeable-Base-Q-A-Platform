package com.example.aikb.service.admin;

import com.example.aikb.entity.User;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.UserMapper;
import com.example.aikb.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 管理端权限校验服务，统一收口 /api/admin 下接口的管理员校验逻辑。
 */
@Service
@RequiredArgsConstructor
public class AdminPermissionService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final UserMapper userMapper;

    /**
     * 校验当前登录用户是否为启用状态的管理员。
     */
    public void ensureAdmin() {
        Long userId = CurrentUser.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1
                || !ADMIN_ROLE.equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(40300, "无权访问管理端接口");
        }
    }
}
