package com.example.aikb.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.rag.retrieval")
public class AppRagRetrievalProperties {

    @Min(1)
    @Max(20)
    private int topK = 5;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double minEffectiveScore = 0.2D;
}
