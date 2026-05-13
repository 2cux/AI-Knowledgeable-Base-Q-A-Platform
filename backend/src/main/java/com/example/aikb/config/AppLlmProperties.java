package com.example.aikb.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * External LLM API configuration.
 */
@Data
@ConfigurationProperties(prefix = "app.llm")
public class AppLlmProperties {

    private boolean enabled = true;

    private String provider = "vendor-http";

    private String baseUrl = "";

    private String apiKey = "";

    private String model = "your-chat-model";

    private Integer maxTokens = 1024;

    private Duration connectTimeout = Duration.ofSeconds(5);

    private Duration readTimeout = Duration.ofSeconds(60);
}
