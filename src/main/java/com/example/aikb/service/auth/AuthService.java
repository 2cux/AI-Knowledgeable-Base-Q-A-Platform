package com.example.aikb.service.auth;

import com.example.aikb.dto.auth.RegisterRequest;

public interface AuthService {

    Long register(RegisterRequest request);
}
