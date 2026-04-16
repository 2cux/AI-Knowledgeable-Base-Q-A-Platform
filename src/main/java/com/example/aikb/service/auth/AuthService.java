package com.example.aikb.service.auth;

import com.example.aikb.dto.auth.LoginRequest;
import com.example.aikb.dto.auth.RegisterRequest;
import com.example.aikb.vo.auth.LoginResponse;

/**
 * 认证业务服务，提供用户注册和登录能力。
 */
public interface AuthService {

    /**
     * 注册新用户。
     *
     * @param request 注册请求参数
     * @return 注册成功后的用户 ID
     */
    Long register(RegisterRequest request);

    /**
     * 用户登录。
     *
     * @param request 登录请求参数
     * @return 登录成功后的令牌和用户信息
     */
    LoginResponse login(LoginRequest request);
}
