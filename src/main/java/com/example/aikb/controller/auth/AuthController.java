package com.example.aikb.controller.auth;

import com.example.aikb.common.Result;
import com.example.aikb.dto.auth.LoginRequest;
import com.example.aikb.dto.auth.RegisterRequest;
import com.example.aikb.service.auth.AuthService;
import com.example.aikb.vo.auth.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口控制器，负责处理用户注册和登录请求。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户注册、登录等认证接口")
public class AuthController {

    private final AuthService authService;

    /**
     * 注册新用户。
     */
    @Operation(summary = "用户注册", description = "创建新用户账号，返回注册成功后的用户 ID")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = authService.register(request);
        return Result.success(userId, "注册成功");
    }

    /**
     * 用户登录并获取访问令牌。
     */
    @Operation(summary = "用户登录", description = "校验用户名和密码，登录成功后返回 JWT 访问令牌")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response, "登录成功");
    }
}
