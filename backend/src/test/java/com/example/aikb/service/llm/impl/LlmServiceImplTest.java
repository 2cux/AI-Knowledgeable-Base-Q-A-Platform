package com.example.aikb.service.llm.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.aikb.config.AppLlmProperties;
import com.example.aikb.dto.llm.request.LlmMessageRequest;
import com.example.aikb.dto.llm.request.LlmRequest;
import com.example.aikb.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LlmServiceImplTest {

    @Test
    void shouldFailWithClearMessageWhenApiKeyMissing() {
        AppLlmProperties properties = new AppLlmProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://llm.vendor.test");
        properties.setApiKey("");
        properties.setModel("claude-opus-4-6");

        LlmServiceImpl service = new LlmServiceImpl(properties, mock(LlmApiClient.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.chatText("hello"));

        assertEquals("LLM api-key 未配置，请通过环境变量 APP_LLM_API_KEY 注入", exception.getMessage());
    }

    @Test
    void shouldMoveSystemMessageToClaudeTopLevelSystemField() {
        AppLlmProperties properties = new AppLlmProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://llm.vendor.test");
        properties.setApiKey("test-key");
        properties.setModel("claude-3-5-sonnet-20240620");

        LlmApiClient apiClient = mock(LlmApiClient.class);
        when(apiClient.chat(any(LlmRequest.class))).thenReturn("{\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}");
        LlmServiceImpl service = new LlmServiceImpl(properties, apiClient);

        service.chat(List.of(
                LlmMessageRequest.builder().role("system").content("system prompt").build(),
                LlmMessageRequest.builder().role("user").content("hello").build()));

        ArgumentCaptor<LlmRequest> requestCaptor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(apiClient).chat(requestCaptor.capture());
        LlmRequest request = requestCaptor.getValue();
        assertEquals("system prompt", request.getSystem());
        assertEquals(1, request.getMessages().size());
        assertEquals("user", request.getMessages().get(0).getRole());
        assertEquals("hello", request.getMessages().get(0).getContent());
    }
}
