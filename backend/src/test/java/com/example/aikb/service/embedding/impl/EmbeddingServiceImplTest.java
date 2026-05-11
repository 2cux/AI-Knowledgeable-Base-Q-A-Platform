package com.example.aikb.service.embedding.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.aikb.config.AppEmbeddingProperties;
import com.example.aikb.dto.embedding.response.EmbeddingData;
import com.example.aikb.dto.embedding.response.EmbeddingResponse;
import com.example.aikb.dto.embedding.response.EmbeddingUsage;
import com.example.aikb.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmbeddingServiceImplTest {

    @Test
    void shouldFailWithClearMessageWhenApiKeyMissing() {
        AppEmbeddingProperties properties = new AppEmbeddingProperties();
        properties.setBaseUrl("https://example.com/v1/embeddings");
        properties.setApiKey("");
        properties.setModel("text-embedding-3-large");
        properties.setNormalized(true);
        properties.setEmbeddingType("float");

        EmbeddingServiceImpl service = new EmbeddingServiceImpl(properties, mock(EmbeddingApiClient.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.embedText("hello"));

        assertEquals("embedding api-key 未配置，请通过环境变量 APP_EMBEDDING_API_KEY 或 OPENAI_API_KEY 注入",
                exception.getMessage());
    }

    @Test
    void shouldReturnVectorWhenDimensionMatchesConfiguredVectorSize() {
        AppEmbeddingProperties properties = validProperties();
        properties.setVectorSize(3);
        EmbeddingApiClient apiClient = mock(EmbeddingApiClient.class);
        when(apiClient.embed(any())).thenReturn(responseWithVector(List.of(0.1F, 0.2F, 0.3F)));

        EmbeddingServiceImpl service = new EmbeddingServiceImpl(properties, apiClient);

        List<Float> vector = service.embedText("hello");

        assertEquals(List.of(0.1F, 0.2F, 0.3F), vector);
    }

    @Test
    void shouldFailWhenVectorDimensionDoesNotMatchConfiguredVectorSize() {
        AppEmbeddingProperties properties = validProperties();
        properties.setVectorSize(3);
        EmbeddingApiClient apiClient = mock(EmbeddingApiClient.class);
        when(apiClient.embed(any())).thenReturn(responseWithVector(List.of(0.1F, 0.2F)));

        EmbeddingServiceImpl service = new EmbeddingServiceImpl(properties, apiClient);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.embedText("hello"));

        assertEquals("Embedding 向量维度不一致，模型：text-embedding-3-large，期望维度：3，实际维度：2",
                exception.getMessage());
    }

    @Test
    void shouldFailWhenVectorIsEmpty() {
        AppEmbeddingProperties properties = validProperties();
        properties.setVectorSize(3);
        EmbeddingApiClient apiClient = mock(EmbeddingApiClient.class);
        when(apiClient.embed(any())).thenReturn(responseWithVector(List.of()));

        EmbeddingServiceImpl service = new EmbeddingServiceImpl(properties, apiClient);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.embedText("hello"));

        assertEquals("Embedding 向量维度不一致，模型：text-embedding-3-large，期望维度：3，实际维度：0",
                exception.getMessage());
    }

    private AppEmbeddingProperties validProperties() {
        AppEmbeddingProperties properties = new AppEmbeddingProperties();
        properties.setBaseUrl("https://example.com/v1/embeddings");
        properties.setApiKey("test-key");
        properties.setModel("text-embedding-3-large");
        properties.setNormalized(true);
        properties.setEmbeddingType("float");
        return properties;
    }

    private EmbeddingResponse responseWithVector(List<Float> vector) {
        EmbeddingData data = new EmbeddingData();
        data.setEmbedding(vector);

        EmbeddingUsage usage = new EmbeddingUsage();
        usage.setPromptTokens(1);
        usage.setTotalTokens(1);

        EmbeddingResponse response = new EmbeddingResponse();
        response.setModel("text-embedding-3-large");
        response.setData(List.of(data));
        response.setUsage(usage);
        return response;
    }
}
