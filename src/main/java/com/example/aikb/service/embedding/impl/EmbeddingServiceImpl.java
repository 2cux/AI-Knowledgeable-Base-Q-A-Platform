package com.example.aikb.service.embedding.impl;

import com.example.aikb.config.AppEmbeddingProperties;
import com.example.aikb.dto.embedding.request.EmbeddingInputItem;
import com.example.aikb.dto.embedding.request.EmbeddingRequest;
import com.example.aikb.dto.embedding.response.EmbeddingData;
import com.example.aikb.dto.embedding.response.EmbeddingResponse;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.embedding.EmbeddingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 最小 embedding 服务实现，负责组装请求体并解析 data[0].embedding。
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.embedding", name = "enabled", havingValue = "true")
public class EmbeddingServiceImpl implements EmbeddingService {

    private final AppEmbeddingProperties properties;
    private final EmbeddingApiClient embeddingApiClient;

    @Override
    public List<Float> embedText(String text) {
        return embedText(text, properties.getModel());
    }

    @Override
    public List<Float> embedText(String text, String model) {
        validateText(text);
        validateConfig();

        EmbeddingRequest request = EmbeddingRequest.builder()
                .model(resolveModel(model))
                .input(List.of(EmbeddingInputItem.textOnly(text.trim())))
                .normalized(properties.getNormalized())
                .embeddingType(properties.getEmbeddingType())
                .build();

        EmbeddingResponse response = embeddingApiClient.embed(request);
        return extractFirstVector(response);
    }

    private List<Float> extractFirstVector(EmbeddingResponse response) {
        if (response == null) {
            throw new BusinessException(50000, "Embedding API 响应为空");
        }
        if (response.getUsage() == null
                || response.getUsage().getPromptTokens() == null
                || response.getUsage().getTotalTokens() == null) {
            throw new BusinessException(50000, "Embedding API 响应缺少 usage");
        }
        if (response.getData() == null || response.getData().isEmpty()) {
            throw new BusinessException(50000, "Embedding API 响应缺少 data");
        }

        EmbeddingData first = response.getData().get(0);
        if (first == null || first.getEmbedding() == null || first.getEmbedding().isEmpty()) {
            throw new BusinessException(50000, "Embedding API 响应缺少 data[0].embedding");
        }
        return first.getEmbedding();
    }

    private String resolveModel(String model) {
        if (StringUtils.hasText(model)) {
            return model.trim();
        }
        return properties.getModel();
    }

    private void validateText(String text) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("向量化文本不能为空");
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new BusinessException(50000, "embedding base-url 未配置");
        }
        if (!StringUtils.hasText(properties.getApiKey())
                || "YOUR_API_KEY_HERE".equals(properties.getApiKey())) {
            throw new BusinessException(50000, "embedding api-key 未配置");
        }
        if (!StringUtils.hasText(properties.getModel())
                || "your-embedding-model".equals(properties.getModel())) {
            throw new BusinessException(50000, "embedding model 未配置");
        }
    }
}
