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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 最小 embedding 服务实现，负责组装请求体并解析 data[0].embedding。
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.embedding", name = "enabled", havingValue = "true")
public class EmbeddingServiceImpl implements EmbeddingService {

    private static final String EXAMPLE_HOST_MARKER = "example.com";

    private final AppEmbeddingProperties properties;
    private final EmbeddingApiClient embeddingApiClient;

    @Override
    public List<Float> embedText(String text) {
        return embedText(text, properties.getModel());
    }

    @Override
    public List<Float> embedText(String text, String model) {
        return embedText(text, null, model);
    }

    @Override
    public List<Float> embedText(String text, String image, String model) {
        validateText(text);
        return embedRequest(EmbeddingRequest.builder()
                .model(resolveModel(model))
                .input(List.of(text.trim()))
                .normalized(properties.getNormalized())
                .embeddingType(properties.getEmbeddingType())
                .build());
    }

    @Override
    public List<Float> embedInput(List<EmbeddingInputItem> input, String model) {
        return embedRequest(EmbeddingRequest.builder()
                .model(resolveModel(model))
                .input(input)
                .normalized(properties.getNormalized())
                .embeddingType(properties.getEmbeddingType())
                .build());
    }

    @Override
    public List<Float> embedRequest(EmbeddingRequest request) {
        if (request == null) {
            throw new BusinessException("embedding request 不能为空");
        }
        validateInput(request.getInput());
        validateConfig();

        request.setModel(resolveModel(request.getModel()));
        request.setNormalized(request.getNormalized() == null ? properties.getNormalized() : request.getNormalized());
        request.setEmbeddingType(StringUtils.hasText(request.getEmbeddingType())
                ? request.getEmbeddingType().trim()
                : properties.getEmbeddingType());

        EmbeddingResponse response = embeddingApiClient.embed(request);
        return extractFirstVector(response, request.getModel());
    }

    private List<Float> extractFirstVector(EmbeddingResponse response, String requestModel) {
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
        if (first == null) {
            throw new BusinessException(50000, "Embedding API 响应缺少 data[0]");
        }
        List<Float> vector = first.getEmbedding();
        validateVectorDimension(vector, resolveResponseModel(response, requestModel));
        return vector;
    }

    private void validateVectorDimension(List<Float> vector, String model) {
        int expected = properties.getVectorSize();
        int actual = vector == null ? 0 : vector.size();
        if (expected <= 0) {
            throw new BusinessException(50000, "embedding vector-size 配置必须大于 0");
        }
        if (actual == 0) {
            throw new BusinessException(50000, buildVectorSizeMessage(model, expected, actual));
        }
        if (vector.stream().anyMatch(item -> item == null)) {
            throw new BusinessException(50000, "Embedding API 响应 data[0].embedding 包含空值");
        }
        if (actual != expected) {
            log.warn("Embedding vector dimension mismatch. model={}, expected={}, actual={}",
                    model, expected, actual);
            throw new BusinessException(50000, buildVectorSizeMessage(model, expected, actual));
        }
    }

    private String resolveResponseModel(EmbeddingResponse response, String requestModel) {
        if (response != null && StringUtils.hasText(response.getModel())) {
            return response.getModel().trim();
        }
        return resolveModel(requestModel);
    }

    private String buildVectorSizeMessage(String model, int expected, int actual) {
        return "Embedding 向量维度不一致，模型：" + model + "，期望维度：" + expected + "，实际维度：" + actual;
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

    private void validateInput(Object input) {
        if (!(input instanceof List<?> inputList) || inputList.isEmpty()) {
            throw new BusinessException("embedding input 不能为空");
        }
        for (int i = 0; i < inputList.size(); i++) {
            Object item = inputList.get(i);
            if (item == null) {
                throw new BusinessException("embedding input[" + i + "] 不能为空");
            }
            if (item instanceof String text) {
                if (!StringUtils.hasText(text)) {
                    throw new BusinessException("embedding input[" + i + "] 文本不能为空");
                }
            } else if (item instanceof EmbeddingInputItem embeddingInputItem) {
                if (!StringUtils.hasText(embeddingInputItem.getText())) {
                    throw new BusinessException("embedding input[" + i + "].text 不能为空");
                }
                if (embeddingInputItem.getImage() == null) {
                    embeddingInputItem.setImage("");
                } else {
                    embeddingInputItem.setImage(embeddingInputItem.getImage().trim());
                }
            }
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new BusinessException(50000, "embedding base-url 未配置");
        }
        if (properties.getBaseUrl().toLowerCase().contains(EXAMPLE_HOST_MARKER)) {
            throw new BusinessException(50000, "Embedding baseUrl 未配置或仍为占位值，请设置 APP_EMBEDDING_BASE_URL");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException(50000,
                    "embedding api-key 未配置，请通过环境变量 APP_EMBEDDING_API_KEY 或 OPENAI_API_KEY 注入");
        }
        if (!StringUtils.hasText(properties.getModel())
                || "your-embedding-model".equals(properties.getModel())) {
            throw new BusinessException(50000, "embedding model 未配置");
        }
        if (properties.getVectorSize() <= 0) {
            throw new BusinessException(50000, "embedding vector-size 配置必须大于 0");
        }
    }
}
