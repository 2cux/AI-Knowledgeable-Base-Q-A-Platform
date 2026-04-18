package com.example.aikb.service.document;

import com.example.aikb.entity.Document;
import com.example.aikb.exception.BusinessException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * MVP 占位文档解析器：优先使用请求传入的纯文本，否则仅尝试读取本地 txt/md 文件。
 */
@Component
public class SimpleDocumentParser {

    private static final Set<String> READABLE_TEXT_TYPES = Set.of("txt", "md");

    /**
     * 获取文档纯文本内容。
     *
     * @param document 文档实体
     * @param textContent 请求直接传入的纯文本
     * @return 文档纯文本
     */
    public String parse(Document document, String textContent) {
        if (StringUtils.hasText(textContent)) {
            return textContent.trim();
        }
        if (!READABLE_TEXT_TYPES.contains(document.getFileType())) {
            throw new BusinessException("MVP阶段仅支持传入textContent，或读取storagePath指向的txt/md文件");
        }
        return readLocalTextFile(document.getStoragePath());
    }

    /**
     * 从本地路径读取 UTF-8 文本文件。
     *
     * @param storagePath 本地文件路径
     * @return 文件文本内容
     */
    private String readLocalTextFile(String storagePath) {
        try {
            Path path = Path.of(storagePath);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                throw new BusinessException("文档文本文件不存在，请在process请求中传入textContent");
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (InvalidPathException ex) {
            throw new BusinessException("文档存储路径不合法，请在process请求中传入textContent");
        } catch (IOException ex) {
            throw new BusinessException("读取文档文本失败");
        }
    }
}
