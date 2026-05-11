package com.example.aikb.service.embedding.impl;

import com.example.aikb.config.AppEmbeddingProperties;
import com.example.aikb.service.embedding.EmbeddingClient;
import com.example.aikb.service.embedding.EmbeddingResult;
import com.example.aikb.service.embedding.EmbeddingService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 将第三方 embedding 服务适配到项目现有 EmbeddingClient 接口。
 */
@Primary
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.embedding", name = "enabled", havingValue = "true")
public class ExternalEmbeddingClient implements EmbeddingClient {

    private final AppEmbeddingProperties properties;
    private final EmbeddingService embeddingService;

    /**
     * 对单段文本执行向量化，并返回服务提供商生成的向量。
     */
    public List<Float> embedText(String text) {
        return embeddingService.embedText(text);
    }

    @Override
    public EmbeddingResult embed(Long chunkId, String text, String embeddingModel) {
        String model = StringUtils.hasText(embeddingModel) ? embeddingModel.trim() : null;
        List<Float> vector = embeddingService.embedText(text, model);

        return EmbeddingResult.builder()
                .embeddingModel(resolveModel(model))
                .vectorId(resolveVectorId(chunkId, text, resolveModel(model)))
                .vector(toDoubleVector(vector))
                .build();
    }

    private String resolveModel(String model) {
        return StringUtils.hasText(model) ? model.trim() : properties.getModel();
    }

    private List<Double> toDoubleVector(List<Float> vector) {
        return vector.stream()
                .filter(Objects::nonNull)
                .map(Float::doubleValue)
                .toList();
    }

    private String resolveVectorId(Long chunkId, String text, String model) {
        if (chunkId == null) {
            return model + "-query-" + Integer.toHexString(Objects.hashCode(text));
        }
        return model + "-" + chunkId;
    }
}
