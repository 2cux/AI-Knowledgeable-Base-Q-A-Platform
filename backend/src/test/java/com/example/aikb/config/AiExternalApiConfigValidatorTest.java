package com.example.aikb.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AiExternalApiConfigValidatorTest {

    @Test
    void shouldFailFastWhenEmbeddingBaseUrlIsMissing() {
        AppEmbeddingProperties embeddingProperties = validEmbeddingProperties();
        embeddingProperties.setBaseUrl("");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new AiExternalApiConfigValidator(embeddingProperties, disabledLlmProperties())
                        .afterPropertiesSet());

        assertEquals("Embedding baseUrl 未配置或仍为占位值，请设置 APP_EMBEDDING_BASE_URL", exception.getMessage());
    }

    @Test
    void shouldFailFastWhenEmbeddingBaseUrlIsPlaceholder() {
        AppEmbeddingProperties embeddingProperties = validEmbeddingProperties();
        embeddingProperties.setBaseUrl("https://api.example.com/v1/embeddings");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new AiExternalApiConfigValidator(embeddingProperties, disabledLlmProperties())
                        .afterPropertiesSet());

        assertEquals("Embedding baseUrl 未配置或仍为占位值，请设置 APP_EMBEDDING_BASE_URL", exception.getMessage());
    }

    @Test
    void shouldFailFastWhenLlmModelIsPlaceholder() {
        AppLlmProperties llmProperties = validLlmProperties();
        llmProperties.setModel("your-chat-model");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new AiExternalApiConfigValidator(disabledEmbeddingProperties(), llmProperties)
                        .afterPropertiesSet());

        assertEquals("LLM model 仍为占位值，请设置 APP_LLM_MODEL", exception.getMessage());
    }

    private AppEmbeddingProperties validEmbeddingProperties() {
        AppEmbeddingProperties properties = new AppEmbeddingProperties();
        properties.setEnabled(true);
        properties.setProvider("external-http");
        properties.setBaseUrl("https://embedding.vendor.test/v1/embeddings");
        properties.setApiKey("test-key");
        properties.setModel("text-embedding-3-large");
        properties.setVectorSize(3072);
        return properties;
    }

    private AppEmbeddingProperties disabledEmbeddingProperties() {
        AppEmbeddingProperties properties = validEmbeddingProperties();
        properties.setEnabled(false);
        return properties;
    }

    private AppLlmProperties validLlmProperties() {
        AppLlmProperties properties = new AppLlmProperties();
        properties.setEnabled(true);
        properties.setProvider("vendor-http");
        properties.setBaseUrl("https://llm.vendor.test/v1/messages");
        properties.setApiKey("test-key");
        properties.setModel("chat-model");
        return properties;
    }

    private AppLlmProperties disabledLlmProperties() {
        AppLlmProperties properties = validLlmProperties();
        properties.setEnabled(false);
        return properties;
    }
}
