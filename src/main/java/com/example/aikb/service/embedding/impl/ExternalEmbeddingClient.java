package com.example.aikb.service.embedding.impl;

import com.example.aikb.config.AppEmbeddingProperties;
import com.example.aikb.dto.embedding.request.EmbeddingInputItem;
import com.example.aikb.dto.embedding.request.EmbeddingRequest;
import com.example.aikb.dto.embedding.response.EmbeddingData;
import com.example.aikb.dto.embedding.response.EmbeddingResponse;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.embedding.EmbeddingClient;
import com.example.aikb.service.embedding.EmbeddingResult;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 外部 embedding API 客户端。
 */
@Slf4j
@Primary
@Component
@ConditionalOnProperty(prefix = "app.embedding", name = "enabled", havingValue = "true")
public class ExternalEmbeddingClient implements EmbeddingClient {

    private final AppEmbeddingProperties properties;
    private final RestTemplate restTemplate;

    public ExternalEmbeddingClient(
            AppEmbeddingProperties properties,
            @Qualifier("embeddingRestTemplate") RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    /**
     * 对单段文本执行向量化，并返回服务提供商生成的向量。
     */
    public List<Float> embedText(String text) {
        validateText(text);
        validateConfig();

        EmbeddingRequest request = EmbeddingRequest.builder()
                .model(properties.getModel())
                .input(List.of(EmbeddingInputItem.textOnly(text.trim())))
                .normalized(properties.getNormalized())
                .embeddingType(properties.getEmbeddingType())
                .build();

        EmbeddingResponse response = callEmbeddingApi(request);
        return extractVector(response);
    }

    @Override
    public EmbeddingResult embed(Long chunkId, String text, String embeddingModel) {
        validateText(text);
        validateConfig();

        String model = StringUtils.hasText(embeddingModel) ? embeddingModel.trim() : properties.getModel();
        EmbeddingRequest request = EmbeddingRequest.builder()
                .model(model)
                .input(List.of(EmbeddingInputItem.textOnly(text.trim())))
                .normalized(properties.getNormalized())
                .embeddingType(properties.getEmbeddingType())
                .build();

        EmbeddingResponse response = callEmbeddingApi(request);
        List<Float> vector = extractVector(response);

        return EmbeddingResult.builder()
                .embeddingModel(StringUtils.hasText(response.getModel()) ? response.getModel() : model)
                .vectorId(resolveVectorId(response, chunkId, model))
                .vector(toDoubleVector(vector))
                .build();
    }

    private EmbeddingResponse callEmbeddingApi(EmbeddingRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getApiKey().trim());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<EmbeddingResponse> response = restTemplate.exchange(
                    properties.getBaseUrl(),
                    HttpMethod.POST,
                    entity,
                    EmbeddingResponse.class);
            return response.getBody();
        } catch (RestClientException ex) {
            log.warn("Embedding API request failed, model={}, error={}", request.getModel(), ex.getMessage());
            throw new BusinessException(50000, "Embedding API request failed");
        }
    }

    private List<Float> extractVector(EmbeddingResponse response) {
        if (response == null) {
            throw new BusinessException(50000, "Embedding API returned empty response");
        }
        if (response.getError() != null && StringUtils.hasText(response.getError().getMessage())) {
            throw new BusinessException(50000, "Embedding API error: " + response.getError().getMessage());
        }
        if (response.getData() != null) {
            for (EmbeddingData item : response.getData()) {
                if (item != null && item.getEmbedding() != null && !item.getEmbedding().isEmpty()) {
                    return item.getEmbedding();
                }
            }
        }
        if (response.getEmbedding() != null && !response.getEmbedding().isEmpty()) {
            return response.getEmbedding();
        }
        throw new BusinessException(50000, "Embedding API returned no vector");
    }

    private List<Double> toDoubleVector(List<Float> vector) {
        return vector.stream()
                .filter(Objects::nonNull)
                .map(Float::doubleValue)
                .toList();
    }

    private String resolveVectorId(EmbeddingResponse response, Long chunkId, String model) {
        if (StringUtils.hasText(response.getId())) {
            return response.getId();
        }
        if (StringUtils.hasText(response.getRequestId())) {
            return response.getRequestId();
        }
        if (chunkId == null) {
            return model + "-query-" + System.nanoTime();
        }
        return model + "-" + chunkId;
    }

    private void validateText(String text) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("Embedding text cannot be empty");
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getBaseUrl())) {
            throw new BusinessException(50000, "Embedding API baseUrl is not configured");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException(50000, "Embedding API apiKey is not configured");
        }
        if (!StringUtils.hasText(properties.getModel())) {
            throw new BusinessException(50000, "Embedding model is not configured");
        }
    }
}
