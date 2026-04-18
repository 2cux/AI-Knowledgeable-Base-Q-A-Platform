package com.example.aikb.service.document;

import com.example.aikb.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 简单文本切片器，按固定字符长度和 overlap 生成切片。
 */
@Component
public class TextSplitter {

    /**
     * 按固定长度切分文本，并保留相邻切片的重叠内容。
     *
     * @param text 待切分文本
     * @param chunkSize 切片长度
     * @param overlap 重叠长度
     * @return 切片文本列表
     */
    public List<String> split(String text, int chunkSize, int overlap) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        if (overlap >= chunkSize) {
            throw new BusinessException("overlap必须小于chunkSize");
        }

        String normalizedText = text.replace("\r\n", "\n").trim();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalizedText.length()) {
            int end = Math.min(start + chunkSize, normalizedText.length());
            String chunk = normalizedText.substring(start, end).trim();
            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }
            if (end >= normalizedText.length()) {
                break;
            }
            start = end - overlap;
        }
        return chunks;
    }
}
