package com.example.aikb.mq.document;

import com.example.aikb.config.AppDocumentProcessMqProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentProcessProducer {

    private final RabbitTemplate rabbitTemplate;
    private final AppDocumentProcessMqProperties properties;

    public void send(DocumentProcessMessage message) {
        rabbitTemplate.convertAndSend(properties.getExchange(), properties.getRoutingKey(), message);
    }
}
