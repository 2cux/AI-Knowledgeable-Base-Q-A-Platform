package com.example.aikb.service.document;

import com.example.aikb.exception.BusinessException;
import java.util.Locale;
import java.util.Set;
import org.springframework.util.StringUtils;

/**
 * Centralized document file type rules for the current P0 flow.
 */
public final class DocumentFileTypeUtils {

    public static final String SUPPORTED_FILE_MESSAGE = "当前仅支持 .txt、.md、.pdf、.docx 文件";
    public static final String SUPPORTED_PARSE_MESSAGE = "当前仅支持 txt、md、pdf、docx 文档解析";
    private static final Set<String> SUPPORTED_TYPES = Set.of("txt", "md", "pdf", "docx");

    private DocumentFileTypeUtils() {
    }

    public static String normalizeAndValidateFileType(String fileType) {
        if (!StringUtils.hasText(fileType)) {
            throw new BusinessException(SUPPORTED_FILE_MESSAGE);
        }
        String normalized = fileType.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_TYPES.contains(normalized)) {
            throw new BusinessException(SUPPORTED_FILE_MESSAGE);
        }
        return normalized;
    }

    public static String resolveAndValidateExtension(String fileName) {
        String safeFileName = validateSafeFileName(fileName);
        int dotIndex = safeFileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == safeFileName.length() - 1) {
            throw new BusinessException(SUPPORTED_FILE_MESSAGE);
        }
        return normalizeAndValidateFileType(safeFileName.substring(dotIndex + 1));
    }

    public static String validateMetadataType(String fileName, String fileType) {
        String extension = resolveAndValidateExtension(fileName);
        String normalizedFileType = normalizeAndValidateFileType(fileType);
        if (!extension.equals(normalizedFileType)) {
            throw new BusinessException("文件名扩展名与 fileType 不一致，" + SUPPORTED_FILE_MESSAGE);
        }
        return normalizedFileType;
    }

    public static void validateProcessSupported(String fileType) {
        if (!StringUtils.hasText(fileType)) {
            throw new BusinessException(SUPPORTED_PARSE_MESSAGE);
        }
        String normalized = fileType.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_TYPES.contains(normalized)) {
            throw new BusinessException(SUPPORTED_PARSE_MESSAGE);
        }
    }

    public static boolean isSupported(String fileType) {
        if (!StringUtils.hasText(fileType)) {
            return false;
        }
        return SUPPORTED_TYPES.contains(fileType.trim().toLowerCase(Locale.ROOT));
    }

    public static String validateSafeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new BusinessException("文件名不能为空，" + SUPPORTED_FILE_MESSAGE);
        }
        String cleanedFileName = StringUtils.cleanPath(fileName.trim());
        if (!StringUtils.hasText(cleanedFileName)
                || cleanedFileName.contains("..")
                || cleanedFileName.contains("/")
                || cleanedFileName.contains("\\")
                || cleanedFileName.length() > 255) {
            throw new BusinessException("文件名非法，" + SUPPORTED_FILE_MESSAGE);
        }
        return cleanedFileName;
    }
}
