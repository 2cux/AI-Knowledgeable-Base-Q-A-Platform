package com.example.aikb.service.embedding;

import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.embedding.mock.MockEmbeddingVectorizer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 占位向量化客户端，不调用真实模型，仅生成稳定的 vectorId，便于 MVP 跑通主流程。
 */
@Component
public class MockEmbeddingClient implements EmbeddingClient {

    /**
     * 生成占位向量 ID。真实实现中这里会调用 embedding 模型并写入向量库。
     *
     * @param chunkId 文档切片 ID
     * @param text 文档切片文本
     * @param embeddingModel 向量化模型名称
     * @return 占位向量化结果
     */
    @Override
    public EmbeddingResult embed(Long chunkId, String text, String embeddingModel) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("chunk内容为空，无法向量化");
        }
        return EmbeddingResult.builder()
                .embeddingModel(embeddingModel)
                .vectorId(embeddingModel + "-" + chunkId)
                .vector(MockEmbeddingVectorizer.vectorize(text))
                .build();
    }
}
