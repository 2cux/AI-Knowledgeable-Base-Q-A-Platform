package com.example.aikb.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "username不能为空")
    @Size(min = 3, max = 20, message = "username长度必须在3到20之间")
    private String username;

    @NotBlank(message = "password不能为空")
    @Size(min = 6, max = 20, message = "password长度必须在6到20之间")
    private String password;

    @NotBlank(message = "confirmPassword不能为空")
    private String confirmPassword;

    @Email(message = "email格式不正确")
    private String email;
}
