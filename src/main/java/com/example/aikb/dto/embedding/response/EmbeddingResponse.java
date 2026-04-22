package com.example.aikb.dto.embedding.response;

import java.util.List;
import lombok.Data;

/**
 * embedding 接口 200 响应体。
 */
@Data
public class EmbeddingResponse {

    /**
     * 响应对象类型，必需字段。
     */
    private String object;

    /**
     * embedding 结果数组，必需字段。
     */
    private List<EmbeddingData> data;

    /**
     * 本次调用使用的模型，必需字段。
     */
    private String model;

    /**
     * Token 用量，必需字段。
     */
    private EmbeddingUsage usage;
}
