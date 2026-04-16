package com.example.aikb.vo.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录响应信息")
public class LoginResponse {

    /** JWT 访问令牌。 */
    @Schema(description = "JWT 访问令牌")
    private String token;

    /** 当前登录用户 ID。 */
    @Schema(description = "当前登录用户 ID", example = "1")
    private Long userId;

    /** 当前登录用户名。 */
    @Schema(description = "当前登录用户名", example = "alice")
    private String username;
}
