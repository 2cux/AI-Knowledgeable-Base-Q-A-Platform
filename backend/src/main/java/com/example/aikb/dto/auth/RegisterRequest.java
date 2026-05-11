package com.example.aikb.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求参数。
 */
@Data
@Schema(description = "用户注册请求参数")
public class RegisterRequest {

    /** 注册用户名。 */
    @Schema(description = "注册用户名，长度 3 到 20 位", example = "alice")
    @NotBlank(message = "username不能为空")
    @Size(min = 3, max = 20, message = "username长度必须在3到20之间")
    private String username;

    /** 登录密码。 */
    @Schema(description = "登录密码，长度 6 到 20 位", example = "123456")
    @NotBlank(message = "password不能为空")
    @Size(min = 6, max = 20, message = "password长度必须在6到20之间")
    private String password;

    /** 确认密码。 */
    @Schema(description = "确认密码，需要与登录密码一致", example = "123456")
    @NotBlank(message = "confirmPassword不能为空")
    private String confirmPassword;

    /** 用户邮箱，可用于后续通知和账号找回。 */
    @Schema(description = "用户邮箱", example = "alice@example.com")
    @Email(message = "email格式不正确")
    private String email;
}
