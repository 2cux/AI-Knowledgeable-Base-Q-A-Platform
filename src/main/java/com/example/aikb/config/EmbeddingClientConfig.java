package com.example.aikb.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * embedding API 调用使用的 HTTP 客户端配置。
 */
@Configuration(proxyBeanMethods = false)
public class EmbeddingClientConfig {

    @Bean
    public RestTemplate embeddingRestTemplate(
            RestTemplateBuilder builder,
            AppEmbeddingProperties properties) {
        return builder
                .connectTimeout(properties.getConnectTimeout())
                .readTimeout(properties.getReadTimeout())
                .build();
    }
}
