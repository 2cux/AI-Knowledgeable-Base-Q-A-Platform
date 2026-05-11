package com.example.aikb.dto.embedding.response;

import java.util.List;
import lombok.Data;

/**
 * embedding 响应中的 data 单项。
 */
@Data
public class EmbeddingData {

    /**
     * 响应对象类型，可选字段。
     */
    private String object;

    /**
     * 服务端返回的向量，可选字段。
     */
    private List<Float> embedding;

    /**
     * 当前向量在 data 数组中的下标，可选字段。
     */
    private Integer index;
}
