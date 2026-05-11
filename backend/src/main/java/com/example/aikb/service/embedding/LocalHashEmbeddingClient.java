package com.example.aikb.service.embedding;

import com.example.aikb.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 本地真实向量生成客户端。
 *
 * <p>MVP 阶段不依赖第三方 SDK，使用文本 token 哈希生成稳定数值向量并落库；
 * 后续接入外部 embedding 服务时替换 EmbeddingClient 实现即可。</p>
 */
@Primary
@Component
@ConditionalOnProperty(prefix = "app.embedding", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalHashEmbeddingClient implements EmbeddingClient {

    private static final int DIMENSION = 128;

    @Override
    public EmbeddingResult embed(Long chunkId, String text, String embeddingModel) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("chunk内容为空，无法向量化");
        }
        String model = StringUtils.hasText(embeddingModel) ? embeddingModel.trim() : "local-hash-embedding-v1";
        String normalizedText = text.trim().toLowerCase();
        List<Double> vector = normalize(vectorize(normalizedText));

        return EmbeddingResult.builder()
                .embeddingModel(model)
                .vectorId(buildVectorId(chunkId, normalizedText, model))
                .vector(vector)
                .build();
    }

    /**
     * 使用词/字符 n-gram 哈希生成固定维度向量。
     */
    private double[] vectorize(String text) {
        double[] values = new double[DIMENSION];
        String[] tokens = text.split("\\s+");
        if (tokens.length > 1) {
            for (String token : tokens) {
                addFeature(values, token, 1.0D);
            }
        }

        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (Character.isWhitespace(current)) {
                continue;
            }
            addFeature(values, String.valueOf(current), 0.25D);
            if (i + 1 < text.length()) {
                addFeature(values, text.substring(i, i + 2), 0.5D);
            }
            if (i + 2 < text.length()) {
                addFeature(values, text.substring(i, i + 3), 0.75D);
            }
        }
        return values;
    }

    private void addFeature(double[] values, String feature, double weight) {
        if (!StringUtils.hasText(feature)) {
            return;
        }
        int index = Math.floorMod(feature.hashCode(), DIMENSION);
        values[index] += weight;
    }

    private List<Double> normalize(double[] values) {
        double norm = 0D;
        for (double value : values) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);

        List<Double> vector = new ArrayList<>(values.length);
        for (double value : values) {
            vector.add(norm == 0D ? 0D : value / norm);
        }
        return vector;
    }

    private String buildVectorId(Long chunkId, String text, String model) {
        String hash = sha256(model + ":" + text).substring(0, 16);
        if (chunkId == null) {
            return model + "-query-" + hash;
        }
        return model + "-" + chunkId + "-" + hash;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(50000, "向量ID生成失败");
        }
    }
}
