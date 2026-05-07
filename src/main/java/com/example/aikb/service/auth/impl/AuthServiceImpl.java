package com.example.aikb.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aikb.common.JwtUtil;
import com.example.aikb.dto.auth.LoginRequest;
import com.example.aikb.dto.auth.RegisterRequest;
import com.example.aikb.entity.User;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.UserMapper;
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.auth.AuthService;
import com.example.aikb.vo.auth.CurrentUserVO;
import com.example.aikb.vo.auth.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 认证业务实现类，负责用户注册、密码校验和登录令牌生成。
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int USER_STATUS_ENABLED = 1;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 注册新用户，校验确认密码和用户名唯一性后写入用户信息。
     *
     * @param request 注册请求参数
     * @return 注册成功后的用户 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }

        Long existingCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setNickname(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }

        int rows = userMapper.insert(user);
        if (rows != 1 || user.getId() == null) {
            throw new BusinessException("用户注册失败");
        }
        return user.getId();
    }

    /**
     * 校验用户登录凭证，登录成功后生成 JWT 访问令牌。
     *
     * @param request 登录请求参数
     * @return 登录响应，包含访问令牌和基础用户信息
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .last("LIMIT 1"));

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    /**
     * 根据安全上下文中的用户 ID 查询当前登录用户基础信息。
     *
     * @return 当前登录用户基础信息
     */
    @Override
    public CurrentUserVO getCurrentUser() {
        Long userId = CurrentUser.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40400, "当前用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != USER_STATUS_ENABLED) {
            throw new BusinessException(40300, "当前用户已被禁用");
        }

        return CurrentUserVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
