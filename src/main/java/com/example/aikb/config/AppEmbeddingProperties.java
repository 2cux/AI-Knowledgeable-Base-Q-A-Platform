package com.example.aikb.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 外部 embedding API 配置。
 */
@Data
@ConfigurationProperties(prefix = "app.embedding")
public class AppEmbeddingProperties {

    /**
     * 是否启用外部 embedding API。为 false 时使用本地 hash 向量客户端。
     */
    private boolean enabled = true;

    /**
     * 完整的 embedding API 地址，例如：https://example.com/v1/embeddings。
     */
    private String baseUrl = "https://your-vendor-base-url";

    /**
     * embedding 服务提供商使用的认证令牌。
     */
    private String apiKey = "YOUR_API_KEY_HERE";

    /**
     * embedding 模型名称。放在配置中便于不同环境切换模型，无需修改代码。
     */
    private String model = "your-embedding-model";

    /**
     * 是否要求服务提供商返回归一化后的向量。
     */
    private Boolean normalized = true;

    /**
     * 服务提供商定义的 embedding 类型，具体取值需要以实际接口文档为准。
     */
    private String embeddingType = "float";

    /**
     * HTTP 连接超时时间。
     */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * HTTP 响应读取超时时间。
     */
    private Duration readTimeout = Duration.ofSeconds(60);
}
