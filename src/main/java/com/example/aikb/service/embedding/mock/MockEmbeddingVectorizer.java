package com.example.aikb.service.embedding.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.util.StringUtils;

/**
 * MVP mock embedding 流程使用的确定性本地向量生成器。
 *
 * <p>该类不是真实语义向量模型，只生成稳定的哈希向量，用于在接入真实 embedding 模型和向量库前跑通检索流程。</p>
 */
public final class MockEmbeddingVectorizer {

    private static final int DIMENSION = 64;

    private MockEmbeddingVectorizer() {
    }

    public static List<Double> vectorize(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }

        double[] values = new double[DIMENSION];
        String normalized = text.trim().toLowerCase();
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (Character.isWhitespace(current)) {
                continue;
            }

            int index = Math.floorMod(current * 31 + i, DIMENSION);
            values[index] += 1.0D;

            // 加入少量二元组特征，让短语顺序对分数产生轻微影响。
            if (i + 1 < normalized.length()) {
                char next = normalized.charAt(i + 1);
                int bigramIndex = Math.floorMod(current * 131 + next * 17, DIMENSION);
                values[bigramIndex] += 0.35D;
            }
        }

        List<Double> vector = new ArrayList<>(DIMENSION);
        for (double value : values) {
            vector.add(value);
        }
        return vector;
    }
}
