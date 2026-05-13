package com.example.aikb.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fails startup when real external AI providers are enabled without usable configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiExternalApiConfigValidator implements InitializingBean {

    private static final String EMBEDDING_PROVIDER_EXTERNAL_HTTP = "external-http";
    private static final String LLM_PROVIDER_VENDOR_HTTP = "vendor-http";
    private static final String EXAMPLE_HOST_MARKER = "example.com";
    private static final String LLM_PLACEHOLDER_MODEL = "your-chat-model";

    private final AppEmbeddingProperties embeddingProperties;
    private final AppLlmProperties llmProperties;

    @Override
    public void afterPropertiesSet() {
        validateEmbedding();
        validateLlm();
    }

    private void validateEmbedding() {
        if (!embeddingProperties.isEnabled() || !isProvider(embeddingProperties.getProvider(), EMBEDDING_PROVIDER_EXTERNAL_HTTP)) {
            return;
        }
        requireConfigured(
                embeddingProperties.getBaseUrl(),
                "Embedding baseUrl 未配置或仍为占位值，请设置 APP_EMBEDDING_BASE_URL");
        requireNotPlaceholder(
                embeddingProperties.getBaseUrl(),
                "Embedding baseUrl 未配置或仍为占位值，请设置 APP_EMBEDDING_BASE_URL");
        requireConfigured(
                embeddingProperties.getApiKey(),
                "Embedding apiKey 未配置，请设置 APP_EMBEDDING_API_KEY");
        requireConfigured(
                embeddingProperties.getModel(),
                "Embedding model 未配置，请设置 APP_EMBEDDING_MODEL");
        if (embeddingProperties.getVectorSize() <= 0) {
            fail("Embedding vectorSize 配置非法，请设置 APP_EMBEDDING_VECTOR_SIZE 为正整数");
        }
    }

    private void validateLlm() {
        if (!llmProperties.isEnabled() || !isProvider(llmProperties.getProvider(), LLM_PROVIDER_VENDOR_HTTP)) {
            return;
        }
        requireConfigured(
                llmProperties.getBaseUrl(),
                "LLM baseUrl 未配置或仍为占位值，请设置 APP_LLM_BASE_URL");
        requireNotPlaceholder(
                llmProperties.getBaseUrl(),
                "LLM baseUrl 未配置或仍为占位值，请设置 APP_LLM_BASE_URL");
        requireConfigured(
                llmProperties.getApiKey(),
                "LLM apiKey 未配置，请设置 APP_LLM_API_KEY");
        requireConfigured(
                llmProperties.getModel(),
                "LLM model 未配置，请设置 APP_LLM_MODEL");
        if (LLM_PLACEHOLDER_MODEL.equalsIgnoreCase(llmProperties.getModel().trim())) {
            fail("LLM model 仍为占位值，请设置 APP_LLM_MODEL");
        }
    }

    private boolean isProvider(String actualProvider, String expectedProvider) {
        return expectedProvider.equalsIgnoreCase(StringUtils.hasText(actualProvider) ? actualProvider.trim() : "");
    }

    private void requireConfigured(String value, String message) {
        if (!StringUtils.hasText(value)) {
            fail(message);
        }
    }

    private void requireNotPlaceholder(String value, String message) {
        if (value != null && value.toLowerCase().contains(EXAMPLE_HOST_MARKER)) {
            fail(message);
        }
    }

    private void fail(String message) {
        log.warn(message);
        throw new IllegalStateException(message);
    }
}
