package com.example.aikb.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录用户上下文，保存通过 JWT 解析出的基础用户信息。
 */
@Data
@AllArgsConstructor
public class LoginUser {

    /** 当前登录用户 ID。 */
    private Long userId;

    /** 当前登录用户名。 */
    private String username;
}
