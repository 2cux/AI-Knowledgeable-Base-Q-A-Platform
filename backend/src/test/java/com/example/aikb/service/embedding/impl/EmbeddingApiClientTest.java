package com.example.aikb.service.embedding.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.aikb.config.AppEmbeddingProperties;
import com.example.aikb.dto.embedding.request.EmbeddingRequest;
import com.example.aikb.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

class EmbeddingApiClientTest {

    @Test
    void shouldNotLeakRequestOrResponseBodyWhenHttpCallFails() {
        AppEmbeddingProperties properties = new AppEmbeddingProperties();
        properties.setBaseUrl("https://embedding.vendor.test/v1/embeddings");
        properties.setApiKey("test-key");
        properties.setModel("text-embedding-3-large");
        properties.setNormalized(true);
        properties.setEmbeddingType("float");

        RestTemplate restTemplate = mock(RestTemplate.class);
        RestClientResponseException exception = new RestClientResponseException(
                "upstream error",
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                null,
                "{\"error\":\"bad api key\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);
        when(restTemplate.postForEntity(eq("https://embedding.vendor.test/v1/embeddings"), any(),
                eq(com.example.aikb.dto.embedding.response.EmbeddingResponse.class)))
                .thenThrow(exception);

        EmbeddingApiClient client = new EmbeddingApiClient(properties, restTemplate);
        EmbeddingRequest request = EmbeddingRequest.builder()
                .model("text-embedding-3-large")
                .input(List.of("secret user content"))
                .normalized(true)
                .embeddingType("float")
                .build();

        BusinessException businessException = assertThrows(BusinessException.class, () -> client.embed(request));

        assertEquals("Embedding API 调用失败: HTTP 401 Unauthorized", businessException.getMessage());
        assertFalse(businessException.getMessage().contains("secret user content"));
        assertFalse(businessException.getMessage().contains("bad api key"));
    }
}
