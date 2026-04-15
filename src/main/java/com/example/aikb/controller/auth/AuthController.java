package com.example.aikb.controller.auth;

import com.example.aikb.common.Result;
import com.example.aikb.dto.auth.LoginRequest;
import com.example.aikb.dto.auth.RegisterRequest;
import com.example.aikb.service.auth.AuthService;
import com.example.aikb.vo.auth.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        Long userId = authService.register(request);
        return Result.success(userId, "注册成功");
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response, "登录成功");
    }
}
