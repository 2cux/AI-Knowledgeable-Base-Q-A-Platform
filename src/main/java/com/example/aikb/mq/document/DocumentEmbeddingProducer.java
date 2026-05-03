package com.example.aikb.mq.document;

import com.example.aikb.config.AppDocumentEmbeddingMqProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentEmbeddingProducer {

    private final RabbitTemplate rabbitTemplate;
    private final AppDocumentEmbeddingMqProperties properties;

    public void send(DocumentEmbeddingMessage message) {
        rabbitTemplate.convertAndSend(properties.getExchange(), properties.getRoutingKey(), message);
    }
}
