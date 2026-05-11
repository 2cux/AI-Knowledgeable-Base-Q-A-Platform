package com.example.aikb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.mq.document-process")
public class AppDocumentProcessMqProperties {

    private String exchange = "aikb.document.exchange";

    private String queue = "aikb.document.process.queue";

    private String routingKey = "aikb.document.process";

    private String deadLetterExchange = "aikb.document.dlx";

    private String deadLetterQueue = "aikb.document.process.dlq";

    private String deadLetterRoutingKey = "aikb.document.process.dlq";
}
