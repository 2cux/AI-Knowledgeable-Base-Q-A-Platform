package com.example.aikb.service.system;

import com.example.aikb.vo.system.RuntimeModeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 应用启动后的 AI 运行模式安全摘要日志。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiRuntimeStartupLogger {

    private final AiRuntimeStatusService aiRuntimeStatusService;

    @EventListener(ApplicationReadyEvent.class)
    public void logRuntimeMode() {
        RuntimeModeVO runtimeMode = aiRuntimeStatusService.getRuntimeMode();
        log.info(
                "AI runtime summary: activeProfiles={}, debugAiTestEnabled={}, "
                        + "embeddingMode={}, embeddingProvider={}, embeddingModel={}, embeddingEnabled={}, "
                        + "embeddingBaseUrlConfigured={}, embeddingApiKeyConfigured={}, "
                        + "llmMode={}, llmProvider={}, llmModel={}, llmEnabled={}, "
                        + "llmBaseUrlConfigured={}, llmApiKeyConfigured={}",
                runtimeMode.getActiveProfiles(),
                runtimeMode.isDebugAiTestEnabled(),
                runtimeMode.getEmbedding().getMode(),
                runtimeMode.getEmbedding().getProvider(),
                runtimeMode.getEmbedding().getModel(),
                runtimeMode.getEmbedding().isEnabled(),
                runtimeMode.getEmbedding().isBaseUrlConfigured(),
                runtimeMode.getEmbedding().isApiKeyConfigured(),
                runtimeMode.getLlm().getMode(),
                runtimeMode.getLlm().getProvider(),
                runtimeMode.getLlm().getModel(),
                runtimeMode.getLlm().isEnabled(),
                runtimeMode.getLlm().isBaseUrlConfigured(),
                runtimeMode.getLlm().isApiKeyConfigured());
    }
}
