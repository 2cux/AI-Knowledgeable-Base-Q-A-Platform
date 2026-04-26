package com.example.aikb.service.document;

import com.example.aikb.entity.Document;
import com.example.aikb.exception.BusinessException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * MVP 文档解析器：当前仅支持 txt/md，优先从真实上传文件读取，兼容 textContent 调试入口。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SimpleDocumentParser {

    private static final String METADATA_STORAGE_PREFIX = "metadata/";
    private static final Set<String> SUPPORTED_FILE_TYPES = Set.of("txt", "md");

    private final LocalDocumentStorage localDocumentStorage;

    /**
     * 获取文档纯文本内容。
     *
     * @param document 文档实体
     * @param textContent 请求直接传入的纯文本，主要用于旧元数据文档调试
     * @return 文档纯文本
     */
    public String parse(Document document, String textContent) {
        validateDocument(document);

        if (hasRealStoragePath(document)) {
            return readStoredText(document);
        }

        if (StringUtils.hasText(textContent)) {
            return textContent.trim();
        }
        throw new BusinessException("文档没有可解析的真实文件，请先通过upload-file上传txt或md文件");
    }

    /**
     * 校验文档类型是否属于当前 MVP 支持范围。
     */
    private void validateDocument(Document document) {
        if (document == null) {
            throw new BusinessException(40400, "文档不存在");
        }
        String fileType = document.getFileType();
        if (!StringUtils.hasText(fileType)) {
            throw new BusinessException("文件类型不能为空");
        }
        String normalizedFileType = fileType.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_FILE_TYPES.contains(normalizedFileType)) {
            throw new BusinessException("当前仅支持解析txt、md文件");
        }
    }

    /**
     * 判断 storagePath 是否来自真实上传路径，而不是旧元数据接口的占位路径。
     */
    private boolean hasRealStoragePath(Document document) {
        String storagePath = document.getStoragePath();
        if (!StringUtils.hasText(storagePath)) {
            return false;
        }

        String normalizedStoragePath = storagePath.trim().replace("\\", "/");
        if (normalizedStoragePath.startsWith(METADATA_STORAGE_PREFIX)) {
            return false;
        }

        if (document.getCreatedBy() == null || document.getKnowledgeBaseId() == null) {
            throw new BusinessException("文档存储路径非法");
        }

        String expectedPrefix = document.getCreatedBy() + "/" + document.getKnowledgeBaseId() + "/";
        if (!normalizedStoragePath.startsWith(expectedPrefix)) {
            throw new BusinessException("文档存储路径非法");
        }
        return true;
    }

    /**
     * 按 UTF-8 读取真实上传的 txt/md 文件，md 在 MVP 阶段按纯文本处理。
     */
    private String readStoredText(Document document) {
        Path path = localDocumentStorage.resolveStoredPath(document.getStoragePath());
        if (!Files.exists(path)) {
            throw new BusinessException("文件不存在或已被删除，请重新上传");
        }
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new BusinessException("文档文件不可读");
        }

        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            if (!StringUtils.hasText(text)) {
                throw new BusinessException("文档内容为空，无法生成切片");
            }
            return text.trim();
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            log.warn("Read document file failed, documentId={}, storagePath={}",
                    document.getId(), document.getStoragePath(), ex);
            throw new BusinessException(50000, "文档文件读取失败");
        }
    }
}
