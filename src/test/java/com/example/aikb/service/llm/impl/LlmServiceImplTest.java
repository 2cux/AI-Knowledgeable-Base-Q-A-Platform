package com.example.aikb.service.llm.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.example.aikb.config.AppLlmProperties;
import com.example.aikb.exception.BusinessException;
import org.junit.jupiter.api.Test;

class LlmServiceImplTest {

    @Test
    void shouldFailWithClearMessageWhenApiKeyMissing() {
        AppLlmProperties properties = new AppLlmProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://example.com/v1/messages");
        properties.setApiKey("");
        properties.setModel("claude-opus-4-6");

        LlmServiceImpl service = new LlmServiceImpl(properties, mock(LlmApiClient.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.chatText("hello"));

        assertEquals("LLM api-key 未配置，请通过环境变量 APP_LLM_API_KEY 或 OPENAI_API_KEY 注入", exception.getMessage());
    }
}
