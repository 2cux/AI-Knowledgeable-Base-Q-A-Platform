package com.example.aikb.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "username不能为空")
    private String username;

    @NotBlank(message = "password不能为空")
    private String password;
}
