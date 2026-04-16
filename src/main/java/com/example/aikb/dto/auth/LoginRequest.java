package com.example.aikb.dto.auth;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户登录请求参数。
 */
@Data
@Schema(description = "用户登录请求参数")
public class LoginRequest {

    /** 登录用户名。 */
    @Schema(description = "登录用户名", example = "alice")
    @NotBlank(message = "username不能为空")
    private String username;

    /** 登录密码。 */
    @Schema(description = "登录密码", example = "123456")
    @NotBlank(message = "password不能为空")
    private String password;
}
