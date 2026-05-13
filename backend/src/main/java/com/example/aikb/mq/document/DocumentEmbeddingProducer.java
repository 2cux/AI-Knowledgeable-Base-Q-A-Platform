package com.example.aikb.mq.document;

import com.example.aikb.config.AppDocumentEmbeddingMqProperties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEmbeddingProducer {

    private final RabbitTemplate rabbitTemplate;
    private final AppDocumentEmbeddingMqProperties properties;

    public void send(DocumentEmbeddingMessage message) {
        String correlationId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(correlationId);
        log.info("Send document embedding message, documentId={}, knowledgeBaseId={}, taskId={}, requestId={}, exchange={}, routingKey={}, correlationId={}",
                message == null ? null : message.getDocumentId(),
                message == null ? null : message.getKnowledgeBaseId(),
                message == null ? null : message.getTaskId(),
                message == null ? null : message.getRequestId(),
                properties.getExchange(),
                properties.getRoutingKey(),
                correlationId);
        rabbitTemplate.convertAndSend(properties.getExchange(), properties.getRoutingKey(), message, correlationData);
        waitForConfirm(message, correlationData);
    }

    private void waitForConfirm(DocumentEmbeddingMessage message, CorrelationData correlationData) {
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
            ReturnedMessage returned = correlationData.getReturned();
            if (returned != null) {
                throw new AmqpException("Document embedding message returned by broker, replyCode="
                        + returned.getReplyCode() + ", replyText=" + returned.getReplyText());
            }
            if (!confirm.isAck()) {
                throw new AmqpException("Document embedding message publish not acknowledged: " + confirm.getReason());
            }
            log.info("Document embedding message publish confirmed, documentId={}, taskId={}, requestId={}, correlationId={}",
                    message == null ? null : message.getDocumentId(),
                    message == null ? null : message.getTaskId(),
                    message == null ? null : message.getRequestId(),
                    correlationData.getId());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AmqpException("Interrupted while waiting for document embedding message publish confirm", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new AmqpException("Document embedding message publish confirm failed", ex);
        }
    }
}
