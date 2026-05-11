package com.example.aikb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.mq.document-embedding")
public class AppDocumentEmbeddingMqProperties {

    private String exchange = "aikb.document.exchange";

    private String queue = "aikb.document.embedding.queue";

    private String routingKey = "aikb.document.embedding";

    private String deadLetterExchange = "aikb.document.dlx";

    private String deadLetterQueue = "aikb.document.embedding.dlq";

    private String deadLetterRoutingKey = "aikb.document.embedding.dlq";
}
