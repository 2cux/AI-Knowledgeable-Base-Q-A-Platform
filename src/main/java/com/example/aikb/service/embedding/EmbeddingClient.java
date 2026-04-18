package com.example.aikb.service.embedding;

/**
 * 向量化客户端抽象，后续可替换为真实 embedding 模型和向量库实现。
 */
public interface EmbeddingClient {

    /**
     * 对单个 chunk 文本执行向量化，并返回向量同步结果。
     *
     * @param chunkId 文档切片 ID
     * @param text 文档切片文本
     * @param embeddingModel 向量化模型名称
     * @return 向量化结果
     */
    EmbeddingResult embed(Long chunkId, String text, String embeddingModel);
}
