package com.example.aikb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置元数据，用于声明 application.yml 中的 jwt.* 配置项。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** JWT 签名密钥。 */
    private String secret;

    /** Token 过期时间，单位毫秒。 */
    private Long expiration;

    /** Token 签发方。 */
    private String issuer;
}
