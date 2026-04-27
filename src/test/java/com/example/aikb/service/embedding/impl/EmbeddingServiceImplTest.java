package com.example.aikb.service.embedding.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.example.aikb.config.AppEmbeddingProperties;
import com.example.aikb.exception.BusinessException;
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
}
