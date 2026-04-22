package com.example.aikb.service.embedding;

import java.util.List;

/**
 * 最小 embedding 服务示例，面向业务代码暴露纯文本向量化方法。
 */
public interface EmbeddingService {

    /**
     * 使用配置中的默认模型向量化单段文本。
     *
     * @param text 待向量化文本
     * @return 向量结果
     */
    List<Float> embedText(String text);

    /**
     * 使用指定模型向量化单段文本。
     *
     * @param text 待向量化文本
     * @param model embedding 模型名称
     * @return 向量结果
     */
    List<Float> embedText(String text, String model);
}
