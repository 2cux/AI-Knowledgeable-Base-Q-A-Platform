package com.example.aikb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Debug/test/internal endpoint access switch.
 */
@Data
@ConfigurationProperties(prefix = "app.debug-api")
public class AppDebugApiProperties {

    /**
     * Whether debug/test/internal endpoints may be accessed after admin verification.
     */
    private boolean enabled = false;
}
