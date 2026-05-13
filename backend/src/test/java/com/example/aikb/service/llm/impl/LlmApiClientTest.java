package com.example.aikb.service.llm.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.aikb.config.AppLlmProperties;
import com.example.aikb.dto.llm.request.LlmMessageRequest;
import com.example.aikb.dto.llm.request.LlmRequest;
import com.example.aikb.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

class LlmApiClientTest {

    @Test
    void shouldPostClaudeMessagesRequestToNormalizedConfiguredEndpointUrl() {
        AppLlmProperties properties = properties("  https://api.xiaocaseai.com/messages/  ");
        RestTemplate restTemplate = mock(RestTemplate.class);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        when(restTemplate.postForEntity(eq("https://api.xiaocaseai.com/messages"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}",
                        responseHeaders, HttpStatus.OK));

        LlmApiClient client = new LlmApiClient(properties, restTemplate, new ObjectMapper());

        String response = client.chat(LlmRequest.builder()
                .model("claude-3-5-sonnet-20240620")
                .maxTokens(1024)
                .system("system prompt")
                .messages(List.of(LlmMessageRequest.userText("hello")))
                .build());

        assertEquals("{\"content\":[{\"type\":\"text\",\"text\":\"ok\"}]}", response);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<LlmRequest>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate)
                .postForEntity(eq("https://api.xiaocaseai.com/messages"), entityCaptor.capture(), eq(String.class));
        HttpHeaders requestHeaders = entityCaptor.getValue().getHeaders();
        assertEquals(MediaType.APPLICATION_JSON, requestHeaders.getContentType());
        assertEquals(List.of(MediaType.APPLICATION_JSON), requestHeaders.getAccept());
        assertEquals("Bearer test-key", requestHeaders.getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals("system prompt", entityCaptor.getValue().getBody().getSystem());
        assertEquals("hello", entityCaptor.getValue().getBody().getMessages().get(0).getContent());
    }

    @Test
    void shouldFailWhenResponseContentTypeIsNotJson() {
        AppLlmProperties properties = properties("https://api.xiaocaseai.com/messages");
        RestTemplate restTemplate = mock(RestTemplate.class);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.TEXT_HTML);
        when(restTemplate.postForEntity(eq("https://api.xiaocaseai.com/messages"), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("<html>bad gateway</html>", responseHeaders, HttpStatus.OK));

        LlmApiClient client = new LlmApiClient(properties, restTemplate, new ObjectMapper());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> client.chat(validRequest()));

        assertEquals("LLM API 返回非 JSON 响应", exception.getMessage());
    }

    @Test
    void shouldWrapHttpErrorWithoutLeakingResponseBody() {
        AppLlmProperties properties = properties("https://api.xiaocaseai.com/messages");
        RestTemplate restTemplate = mock(RestTemplate.class);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.TEXT_HTML);
        RestClientResponseException exception = new RestClientResponseException(
                "upstream error",
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                responseHeaders,
                "<html>bad api key</html>".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        when(restTemplate.postForEntity(eq("https://api.xiaocaseai.com/messages"), any(), eq(String.class)))
                .thenThrow(exception);

        LlmApiClient client = new LlmApiClient(properties, restTemplate, new ObjectMapper());

        BusinessException businessException = assertThrows(BusinessException.class,
                () -> client.chat(validRequest()));

        assertEquals("LLM API 调用失败: HTTP 401", businessException.getMessage());
    }

    private AppLlmProperties properties(String baseUrl) {
        AppLlmProperties properties = new AppLlmProperties();
        properties.setBaseUrl(baseUrl);
        properties.setApiKey("test-key");
        properties.setModel("claude-3-5-sonnet-20240620");
        properties.setMaxTokens(1024);
        return properties;
    }

    private LlmRequest validRequest() {
        return LlmRequest.builder()
                .model("claude-3-5-sonnet-20240620")
                .maxTokens(1024)
                .system("system prompt")
                .messages(List.of(LlmMessageRequest.userText("hello")))
                .build();
    }
}
