package com.example.aikb.mq.document;

import com.example.aikb.common.LogSanitizer;
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
            log.info("Rejected invalid document embedding message, deliveryTag={}, requeue=false", deliveryTag);
            return;
        }

        try {
            log.info("Consume document embedding message, documentId={}, knowledgeBaseId={}, taskId={}, requestId={}, threadName={}, deliveryTag={}",
                    payload.getDocumentId(),
                    payload.getKnowledgeBaseId(),
                    payload.getTaskId(),
                    payload.getRequestId(),
                    Thread.currentThread().getName(),
                    deliveryTag);
            documentEmbeddingService.embedDocumentFromMessage(payload);
            channel.basicAck(deliveryTag, false);
            log.info("Ack document embedding message after success, documentId={}, taskId={}, requestId={}, deliveryTag={}",
                    payload.getDocumentId(), payload.getTaskId(), payload.getRequestId(), deliveryTag);
        } catch (RuntimeException ex) {
            log.error("Document embedding message failed, documentId={}, taskId={}, requestId={}, message={}",
                    payload.getDocumentId(),
                    payload.getTaskId(),
                    payload.getRequestId(),
                    LogSanitizer.safeMessage(ex.getMessage()),
                    ex);
            // 业务失败已由 DocumentEmbeddingService 落库为 FAILED；MVP 阶段失败后 ack，避免消息长期 unacked 或无限重试外部 API。
            channel.basicAck(deliveryTag, false);
            log.info("Ack document embedding message after persisted failure, documentId={}, taskId={}, requestId={}, deliveryTag={}",
                    payload.getDocumentId(), payload.getTaskId(), payload.getRequestId(), deliveryTag);
        }
    }
}
