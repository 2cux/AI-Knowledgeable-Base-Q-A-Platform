package com.example.aikb.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * External embedding API configuration.
 */
@Data
@ConfigurationProperties(prefix = "app.embedding")
public class AppEmbeddingProperties {

    private boolean enabled = true;

    private String provider = "external-http";

    private String baseUrl = "";

    private String apiKey = "";

    private String model = "text-embedding-3-large";

    private int vectorSize = 3072;

    private Boolean normalized = true;

    private String embeddingType = "float";

    private Duration connectTimeout = Duration.ofSeconds(10);

    private Duration readTimeout = Duration.ofSeconds(120);
}
