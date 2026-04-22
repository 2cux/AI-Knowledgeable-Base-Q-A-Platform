package com.example.aikb.controller.debug;

import com.example.aikb.common.Result;
import com.example.aikb.config.AppEmbeddingProperties;
import com.example.aikb.config.AppLlmProperties;
import com.example.aikb.dto.embedding.request.EmbeddingInputItem;
import com.example.aikb.dto.embedding.request.EmbeddingRequest;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.service.embedding.EmbeddingService;
import com.example.aikb.service.llm.LlmService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 外部接口联调用临时调试接口。
 *
 * <p>仅在 app.debug.ai-test.enabled=true 时注册，联调结束后建议关闭。</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/debug/ai")
@ConditionalOnProperty(prefix = "app.debug.ai-test", name = "enabled", havingValue = "true")
public class AiDebugController {

    private static final int VECTOR_PREVIEW_SIZE = 5;

    private final ObjectProvider<EmbeddingService> embeddingServiceProvider;
    private final LlmService llmService;
    private final AppEmbeddingProperties embeddingProperties;
    private final AppLlmProperties llmProperties;

    /**
     * 单独测试 embedding 接口是否可用。
     */
    @PostMapping("/embedding")
    public Result<EmbeddingDebugResponse> testEmbedding(@RequestBody(required = false) TextDebugRequest request) {
        EmbeddingService embeddingService = embeddingServiceProvider.getIfAvailable();
        if (embeddingService == null) {
            throw new BusinessException(50000, "EmbeddingService 未注册，请检查 app.embedding.enabled");
        }

        List<Float> vector;
        if (request != null && request.getInput() != null && !request.getInput().isEmpty()) {
            validateDebugInput(request.getInput());
            vector = embeddingService.embedRequest(toEmbeddingRequest(request));
        } else {
            String text = resolveText(request == null ? null : request.getText(), "测试知识库向量化是否接通");
            String image = request == null ? null : request.getImage();
            validateDebugImage(image);
            vector = embeddingService.embedText(text, image, resolveEmbeddingModel(request));
        }
        if (vector == null || vector.isEmpty()) {
            throw new BusinessException(50000, "embedding 返回向量为空");
        }

        return Result.success(EmbeddingDebugResponse.builder()
                .enabled(embeddingProperties.isEnabled())
                .baseUrlConfigured(StringUtils.hasText(embeddingProperties.getBaseUrl()))
                .apiKeyConfigured(isApiKeyConfigured(embeddingProperties.getApiKey()))
                .model(embeddingProperties.getModel())
                .vectorSize(vector.size())
                .vectorPreview(vector.stream().limit(VECTOR_PREVIEW_SIZE).toList())
                .build());
    }

    /**
     * 单独测试 LLM 接口是否可用。
     */
    @PostMapping("/llm")
    public Result<LlmDebugResponse> testLlm(@RequestBody(required = false) TextDebugRequest request) {
        String question = resolveText(request == null ? null : request.getText(), "请用一句话回答：你已经接通了吗？");
        JsonNode response = llmService.chatText(question);
        if (response == null || response.isNull() || response.isMissingNode()) {
            throw new BusinessException(50000, "LLM 返回结果为空");
        }

        String rawResponse = response.toString();
        if (!StringUtils.hasText(rawResponse) || "{}".equals(rawResponse)) {
            throw new BusinessException(50000, "LLM 返回结果为空对象，请确认响应结构或上游服务状态");
        }

        return Result.success(LlmDebugResponse.builder()
                .enabled(llmProperties.isEnabled())
                .baseUrlConfigured(StringUtils.hasText(llmProperties.getBaseUrl()))
                .apiKeyConfigured(isApiKeyConfigured(llmProperties.getApiKey()))
                .model(llmProperties.getModel())
                .response(rawResponse)
                .build());
    }

    private String resolveText(String input, String defaultText) {
        return StringUtils.hasText(input) ? input.trim() : defaultText;
    }

    private boolean isApiKeyConfigured(String apiKey) {
        return StringUtils.hasText(apiKey) && !"YOUR_API_KEY_HERE".equals(apiKey);
    }

    private void validateDebugInput(List<EmbeddingInputItem> input) {
        for (int i = 0; i < input.size(); i++) {
            EmbeddingInputItem item = input.get(i);
            if (item != null) {
                validateDebugImage(item.getImage());
            }
        }
    }

    private void validateDebugImage(String image) {
        if ("string".equalsIgnoreCase(image == null ? null : image.trim())) {
            throw new BusinessException(50000,
                    "embedding image不能使用接口文档占位值string，请传真实图片URL/Base64，或确认是否应使用纯文本embedding模型/接口");
        }
    }

    private EmbeddingRequest toEmbeddingRequest(TextDebugRequest request) {
        return EmbeddingRequest.builder()
                .model(resolveEmbeddingModel(request))
                .input(request.getInput())
                .normalized(request.getNormalized() == null ? embeddingProperties.getNormalized() : request.getNormalized())
                .embeddingType(StringUtils.hasText(request.getEmbeddingType())
                        ? request.getEmbeddingType().trim()
                        : embeddingProperties.getEmbeddingType())
                .build();
    }

    private String resolveEmbeddingModel(TextDebugRequest request) {
        if (request != null && StringUtils.hasText(request.getModel())) {
            return request.getModel().trim();
        }
        return embeddingProperties.getModel();
    }

    @Data
    public static class TextDebugRequest {
        /**
         * embedding 测试文本或 LLM 测试问题。不传时使用默认测试文本。
         */
        private String text;

        /**
         * embedding 图片内容或图片地址。上游协议要求 input[].image 为字符串；不传时使用空字符串。
         */
        private String image;

        /**
         * 直接按上游协议透传 input 数组；传了 input 时会优先使用它，忽略 text/image。
         */
        private List<EmbeddingInputItem> input;

        /**
         * 调试时可覆盖配置文件里的 embedding 模型。
         */
        private String model;

        /**
         * 调试时可覆盖 normalized。
         */
        private Boolean normalized;

        /**
         * 调试时可覆盖 embedding_type。
         */
        @JsonProperty("embedding_type")
        private String embeddingType;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class EmbeddingDebugResponse {
        private boolean enabled;
        private boolean baseUrlConfigured;
        private boolean apiKeyConfigured;
        private String model;
        private Integer vectorSize;
        private List<Float> vectorPreview;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class LlmDebugResponse {
        private boolean enabled;
        private boolean baseUrlConfigured;
        private boolean apiKeyConfigured;
        private String model;
        private String response;
    }
}
