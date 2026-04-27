package com.example.aikb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 调试接口开关配置。
 *
 * <p>该开关仅用于本地开发或联调，生产环境默认应保持关闭。</p>
 */
@Data
@ConfigurationProperties(prefix = "app.debug.ai-test")
public class AppDebugAiTestProperties {

    /**
     * 是否显式开启 AI 调试接口。
     *
     * <p>默认关闭；dev 环境会在访问守卫中单独放行，便于本地联调。</p>
     */
    private boolean enabled = false;
}
