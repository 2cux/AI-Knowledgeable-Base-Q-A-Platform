package com.example.aikb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * 文件上传配置，集中管理本地存储根目录和业务层文件大小限制。
 */
@Data
@ConfigurationProperties(prefix = "app.file")
public class AppFileProperties {

    /** 后端受控的文件上传根目录。 */
    private String uploadDir = "uploads";

    /** 单文件最大大小，默认与 Spring multipart 限制保持一致。 */
    private DataSize maxSize = DataSize.ofMegabytes(20);
}
