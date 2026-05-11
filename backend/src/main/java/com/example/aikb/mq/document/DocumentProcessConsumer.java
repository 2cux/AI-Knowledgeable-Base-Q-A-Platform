package com.example.aikb.mq.document;

import com.example.aikb.common.LogSanitizer;
import com.example.aikb.service.document.DocumentProcessService;
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
public class DocumentProcessConsumer {

    private final DocumentProcessService documentProcessService;

    @RabbitListener(queues = "${app.mq.document-process.queue}")
    public void consume(DocumentProcessMessage payload, Message amqpMessage, Channel channel) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
        if (payload == null || payload.getDocumentId() == null) {
            log.warn("Reject invalid document process message, requestId={}",
                    payload == null ? null : payload.getRequestId());
            channel.basicReject(deliveryTag, false);
            return;
        }

        try {
            log.info("Consume document process message, documentId={}, requestId={}",
                    payload.getDocumentId(), payload.getRequestId());
            documentProcessService.processFromMessage(payload);
            channel.basicAck(deliveryTag, false);
        } catch (RuntimeException ex) {
            log.warn("Document process message failed, documentId={}, requestId={}, message={}",
                    payload.getDocumentId(), payload.getRequestId(), LogSanitizer.safeMessage(ex.getMessage()));
            throw ex;
        }
    }
}
