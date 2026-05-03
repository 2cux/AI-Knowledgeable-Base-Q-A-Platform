package com.example.aikb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.cache.admin")
public class AppAdminCacheProperties {

    /**
     * TTL for the base admin chat stats cache.
     */
    private long statsTtlMinutes = 5L;
}
