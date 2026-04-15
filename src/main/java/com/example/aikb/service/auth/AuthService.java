package com.example.aikb.service.auth;

import com.example.aikb.dto.auth.LoginRequest;
import com.example.aikb.dto.auth.RegisterRequest;
import com.example.aikb.vo.auth.LoginResponse;

public interface AuthService {

    Long register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}
