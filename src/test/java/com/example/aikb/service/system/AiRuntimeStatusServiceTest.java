package com.example.aikb.service.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.aikb.config.AppDebugAiTestProperties;
import com.example.aikb.config.AppEmbeddingProperties;
import com.example.aikb.config.AppLlmProperties;
import com.example.aikb.service.debug.AiDebugAccessGuard;
import com.example.aikb.service.embedding.LocalHashEmbeddingClient;
import com.example.aikb.service.llm.LlmClient;
import com.example.aikb.vo.system.RuntimeModeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;

class AiRuntimeStatusServiceTest {

    @Test
    void shouldReportEmbeddingFallbackWhenEmbeddingDisabled() {
        AppEmbeddingProperties embeddingProperties = new AppEmbeddingProperties();
        embeddingProperties.setEnabled(false);
        embeddingProperties.setModel("");

        AppLlmProperties llmProperties = new AppLlmProperties();
        llmProperties.setEnabled(false);

        AppDebugAiTestProperties debugAiTestProperties = new AppDebugAiTestProperties();
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("embeddingClient", new LocalHashEmbeddingClient());

        AiRuntimeStatusService service = new AiRuntimeStatusService(
                embeddingProperties,
                llmProperties,
                new AiDebugAccessGuard(debugAiTestProperties, null, environment),
                beanFactory.getBeanProvider(com.example.aikb.service.embedding.EmbeddingClient.class),
                beanFactory.getBeanProvider(LlmClient.class),
                environment);

        RuntimeModeVO runtimeMode = service.getRuntimeMode();

        assertEquals(AiRuntimeMode.FALLBACK, runtimeMode.getEmbedding().getMode());
        assertEquals("local-hash", runtimeMode.getEmbedding().getProvider());
        assertFalse(runtimeMode.getEmbedding().isEnabled());
        assertTrue(runtimeMode.isDebugAiTestEnabled());
    }

    @Test
    void shouldReportLlmMisconfiguredWhenApiKeyMissing() {
        AppEmbeddingProperties embeddingProperties = new AppEmbeddingProperties();
        embeddingProperties.setEnabled(false);

        AppLlmProperties llmProperties = new AppLlmProperties();
        llmProperties.setEnabled(true);
        llmProperties.setBaseUrl("https://example.com/v1/messages");
        llmProperties.setApiKey("");
        llmProperties.setModel("claude-opus-4-6");

        AppDebugAiTestProperties debugAiTestProperties = new AppDebugAiTestProperties();
        MockEnvironment environment = new MockEnvironment();

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        AiRuntimeStatusService service = new AiRuntimeStatusService(
                embeddingProperties,
                llmProperties,
                new AiDebugAccessGuard(debugAiTestProperties, null, environment),
                beanFactory.getBeanProvider(com.example.aikb.service.embedding.EmbeddingClient.class),
                beanFactory.getBeanProvider(LlmClient.class),
                environment);

        RuntimeModeVO runtimeMode = service.getRuntimeMode();

        assertEquals(AiRuntimeMode.MISCONFIGURED, runtimeMode.getLlm().getMode());
        assertFalse(runtimeMode.getLlm().isApiKeyConfigured());
        assertEquals("default", runtimeMode.getActiveProfiles().get(0));
    }
}
