package com.example.aikb.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM 调用配置，MVP 阶段使用 OpenAI 兼容的 chat completions HTTP 接口。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.llm")
public class AppLlmProperties {

    /** LLM 服务地址，例如 https://api.openai.com/v1/chat/completions 或本地兼容接口。 */
    private String baseUrl;

    /** LLM API Key。若本地服务不需要鉴权，可留空。 */
    private String apiKey;

    /** 使用的模型名称。 */
    private String model = "gpt-4o-mini";

    /** 生成温度，MVP 问答默认偏稳健。 */
    private Double temperature = 0.2D;

    /** 单次回答最大 token 数。 */
    private Integer maxTokens = 800;

    /** 连接超时时间。 */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /** 请求读取超时时间。 */
    private Duration readTimeout = Duration.ofSeconds(60);
}
