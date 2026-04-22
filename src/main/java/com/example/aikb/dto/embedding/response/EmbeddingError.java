package com.example.aikb.dto.embedding.response;

import lombok.Data;

/**
 * 通用错误结构。需要根据服务提供商的真实错误响应调整。
 */
@Data
public class EmbeddingError {

    private String code;

    private String message;

    private String type;
}
