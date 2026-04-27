package com.example.aikb.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 第三方 LLM API 调用配置。
 */
@Data
@ConfigurationProperties(prefix = "app.llm")
public class AppLlmProperties {

    /** 是否启用真实 LLM API 调用。 */
    private boolean enabled = true;

    /** 完整的 LLM API 地址，例如：https://your-vendor-base-url。 */
    private String baseUrl = "https://your-vendor-base-url";

    /**
     * LLM 服务认证密钥。
     *
     * <p>必须通过环境变量注入，例如 APP_LLM_API_KEY 或 OPENAI_API_KEY。</p>
     */
    private String apiKey = "";

    /** LLM 模型名称。 */
    private String model = "claude-3-5-sonnet-20240620";

    /** 单次回答最大 token 数，对应请求体 max_tokens。 */
    private Integer maxTokens = 1024;

    /** HTTP 连接超时时间。 */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /** HTTP 响应读取超时时间。 */
    private Duration readTimeout = Duration.ofSeconds(60);
}
