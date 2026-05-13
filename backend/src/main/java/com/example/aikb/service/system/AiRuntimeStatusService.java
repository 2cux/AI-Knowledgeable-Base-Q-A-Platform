package com.example.aikb.service.system;

import com.example.aikb.config.AppEmbeddingProperties;
import com.example.aikb.config.AppLlmProperties;
import com.example.aikb.service.debug.AiDebugAccessGuard;
import com.example.aikb.service.embedding.EmbeddingClient;
import com.example.aikb.service.embedding.LocalHashEmbeddingClient;
import com.example.aikb.service.embedding.MockEmbeddingClient;
import com.example.aikb.service.embedding.impl.ExternalEmbeddingClient;
import com.example.aikb.service.llm.LlmClient;
import com.example.aikb.service.llm.impl.VendorLlmClientAdapter;
import com.example.aikb.vo.system.AiCapabilityStatusVO;
import com.example.aikb.vo.system.RuntimeModeVO;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AI 运行模式识别服务。
 *
 * <p>该服务只读取配置和当前注入的实现类型，不会主动调用外部模型服务。</p>
 */
@Service
@RequiredArgsConstructor
public class AiRuntimeStatusService {

    private static final String DEFAULT_EMBEDDING_PLACEHOLDER_MODEL = "your-embedding-model";
    private static final String LOCAL_HASH_MODEL = "local-hash-embedding-v1";
    private static final String UNKNOWN_URL_PART = "invalid-url";
    private static final String EXAMPLE_HOST_MARKER = "example.com";
    private static final String LLM_PLACEHOLDER_MODEL = "your-chat-model";

    private final AppEmbeddingProperties embeddingProperties;
    private final AppLlmProperties llmProperties;
    private final AiDebugAccessGuard aiDebugAccessGuard;
    private final ObjectProvider<EmbeddingClient> embeddingClientProvider;
    private final ObjectProvider<LlmClient> llmClientProvider;
    private final Environment environment;

    public RuntimeModeVO getRuntimeMode() {
        return RuntimeModeVO.builder()
                .activeProfiles(resolveActiveProfiles())
                .debugAiTestEnabled(resolveDebugAiTestEnabled())
                .embedding(resolveEmbeddingStatus())
                .llm(resolveLlmStatus())
                .build();
    }

    private List<String> resolveActiveProfiles() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles == null || activeProfiles.length == 0) {
            return List.of("default");
        }
        return Arrays.asList(activeProfiles);
    }

    private boolean resolveDebugAiTestEnabled() {
        return aiDebugAccessGuard.isDebugEnabled();
    }

    private AiCapabilityStatusVO resolveEmbeddingStatus() {
        EmbeddingClient embeddingClient = embeddingClientProvider.getIfAvailable();
        String provider = resolveEmbeddingProvider(embeddingClient);
        boolean enabled = embeddingProperties.isEnabled();
        boolean baseUrlConfigured = StringUtils.hasText(embeddingProperties.getBaseUrl());
        boolean apiKeyConfigured = StringUtils.hasText(embeddingProperties.getApiKey());
        String configuredModel = embeddingProperties.getModel();

        AiRuntimeMode mode;
        String model;
        if (!enabled) {
            mode = AiRuntimeMode.DISABLED;
            model = StringUtils.hasText(configuredModel) && !DEFAULT_EMBEDDING_PLACEHOLDER_MODEL.equals(configuredModel)
                    ? configuredModel
                    : LOCAL_HASH_MODEL;
        } else if (!baseUrlConfigured || isPlaceholderBaseUrl(embeddingProperties.getBaseUrl())
                || !apiKeyConfigured || !StringUtils.hasText(configuredModel)
                || DEFAULT_EMBEDDING_PLACEHOLDER_MODEL.equals(configuredModel)) {
            mode = AiRuntimeMode.MISCONFIGURED;
            model = configuredModel;
        } else if (embeddingClient instanceof MockEmbeddingClient) {
            mode = AiRuntimeMode.MOCK;
            model = configuredModel;
        } else if (embeddingClient instanceof LocalHashEmbeddingClient) {
            mode = AiRuntimeMode.FALLBACK;
            model = StringUtils.hasText(configuredModel) ? configuredModel : LOCAL_HASH_MODEL;
        } else {
            mode = AiRuntimeMode.REAL;
            model = configuredModel;
        }

        return AiCapabilityStatusVO.builder()
                .mode(mode)
                .provider(provider)
                .model(model)
                .enabled(enabled)
                .baseUrlConfigured(baseUrlConfigured)
                .baseUrlHost(resolveUrlHost(embeddingProperties.getBaseUrl()))
                .baseUrlPath(resolveUrlPath(embeddingProperties.getBaseUrl()))
                .apiKeyConfigured(apiKeyConfigured)
                .build();
    }

    private AiCapabilityStatusVO resolveLlmStatus() {
        LlmClient llmClient = llmClientProvider.getIfAvailable();
        boolean enabled = llmProperties.isEnabled();
        boolean baseUrlConfigured = StringUtils.hasText(llmProperties.getBaseUrl());
        boolean apiKeyConfigured = StringUtils.hasText(llmProperties.getApiKey());
        String model = llmProperties.getModel();

        AiRuntimeMode mode;
        if (!enabled) {
            mode = AiRuntimeMode.DISABLED;
        } else if (!baseUrlConfigured || isPlaceholderBaseUrl(llmProperties.getBaseUrl())
                || !apiKeyConfigured || !StringUtils.hasText(model) || LLM_PLACEHOLDER_MODEL.equalsIgnoreCase(model)
                || llmClient == null) {
            mode = AiRuntimeMode.MISCONFIGURED;
        } else {
            mode = AiRuntimeMode.REAL;
        }

        return AiCapabilityStatusVO.builder()
                .mode(mode)
                .provider(resolveLlmProvider(llmClient))
                .model(model)
                .enabled(enabled)
                .baseUrlConfigured(baseUrlConfigured)
                .baseUrlHost(resolveUrlHost(llmProperties.getBaseUrl()))
                .baseUrlPath(resolveUrlPath(llmProperties.getBaseUrl()))
                .apiKeyConfigured(apiKeyConfigured)
                .build();
    }

    private String resolveEmbeddingProvider(EmbeddingClient embeddingClient) {
        if (embeddingProperties.isEnabled() && StringUtils.hasText(embeddingProperties.getProvider())) {
            return embeddingProperties.getProvider().trim();
        }
        if (embeddingClient instanceof ExternalEmbeddingClient) {
            return "external-http";
        }
        if (embeddingClient instanceof LocalHashEmbeddingClient) {
            return "local-hash";
        }
        if (embeddingClient instanceof MockEmbeddingClient) {
            return "mock-embedding";
        }
        return embeddingClient == null ? "unavailable" : embeddingClient.getClass().getSimpleName();
    }

    private String resolveLlmProvider(LlmClient llmClient) {
        if (StringUtils.hasText(llmProperties.getProvider())) {
            return llmProperties.getProvider().trim();
        }
        if (llmClient instanceof VendorLlmClientAdapter) {
            return "vendor-http";
        }
        return llmClient == null ? "unavailable" : llmClient.getClass().getSimpleName();
    }

    private String resolveUrlHost(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "";
        }
        try {
            String host = new URI(baseUrl.trim()).getHost();
            return host == null ? UNKNOWN_URL_PART : host;
        } catch (URISyntaxException ex) {
            return UNKNOWN_URL_PART;
        }
    }

    private String resolveUrlPath(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "";
        }
        try {
            String path = new URI(baseUrl.trim()).getPath();
            return path == null ? "" : path;
        } catch (URISyntaxException ex) {
            return UNKNOWN_URL_PART;
        }
    }

    private boolean isPlaceholderBaseUrl(String baseUrl) {
        return baseUrl != null && baseUrl.toLowerCase().contains(EXAMPLE_HOST_MARKER);
    }
}
