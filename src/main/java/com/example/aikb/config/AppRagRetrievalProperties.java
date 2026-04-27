package com.example.aikb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.rag.retrieval")
public class AppRagRetrievalProperties {

    private int topK = 5;

    private double minEffectiveScore = 0.2D;
}
