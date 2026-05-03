package com.example.aikb.mq.document;

import com.example.aikb.service.embedding.DocumentEmbeddingService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEmbeddingConsumer {

    private final DocumentEmbeddingService documentEmbeddingService;

    @RabbitListener(queues = "${app.mq.document-embedding.queue}")
    public void consume(DocumentEmbeddingMessage payload, Message amqpMessage, Channel channel) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        if (payload == null || payload.getDocumentId() == null) {
            log.warn("Reject invalid document embedding message, requestId={}",
                    payload == null ? null : payload.getRequestId());
            channel.basicReject(deliveryTag, false);
            return;
        }

        try {
            log.info("Consume document embedding message, documentId={}, requestId={}",
                    payload.getDocumentId(), payload.getRequestId());
            documentEmbeddingService.embedDocumentFromMessage(payload);
            channel.basicAck(deliveryTag, false);
        } catch (RuntimeException ex) {
            log.warn("Document embedding message failed, documentId={}, requestId={}, message={}",
                    payload.getDocumentId(), payload.getRequestId(), ex.getMessage());
            throw ex;
        }
    }
}
