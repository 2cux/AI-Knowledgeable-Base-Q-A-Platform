package com.example.aikb.service.document;

import com.example.aikb.config.AppFileProperties;
import com.example.aikb.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 本地文档存储辅助类，负责校验文件名、生成服务端存储路径并保存真实文件。
 */
@Component
@RequiredArgsConstructor
public class LocalDocumentStorage {

    private static final Set<String> SUPPORTED_FILE_TYPES = Set.of("txt", "md");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final AppFileProperties appFileProperties;

    /**
     * 保存上传文件，目录按用户 ID、知识库 ID、日期分层。
     *
     * @param userId 当前用户 ID
     * @param knowledgeBaseId 知识库 ID
     * @param multipartFile 上传文件
     * @param customFileName 可选自定义文件名
     * @return 已保存文件的元信息
     */
    public StoredDocumentFile save(Long userId, Long knowledgeBaseId, MultipartFile multipartFile, String customFileName) {
        validateNotEmpty(multipartFile);
        validateFileSize(multipartFile);

        String originalFileName = resolveOriginalFileName(multipartFile, customFileName);
        String fileType = resolveAndValidateFileType(originalFileName);

        Path uploadRoot = getUploadRoot();
        String date = LocalDate.now().format(DATE_FORMATTER);
        String storedFileName = UUID.randomUUID() + "." + fileType;
        Path targetDirectory = uploadRoot
                .resolve(String.valueOf(userId))
                .resolve(String.valueOf(knowledgeBaseId))
                .resolve(date)
                .normalize();
        Path targetPath = targetDirectory.resolve(storedFileName).normalize();

        if (!targetPath.startsWith(uploadRoot)) {
            throw new BusinessException("文件存储路径非法");
        }

        try {
            Files.createDirectories(targetDirectory);
            multipartFile.transferTo(targetPath);
        } catch (IOException | IllegalStateException ex) {
            deletePathQuietly(targetPath);
            throw new BusinessException(50000, "文件保存失败");
        }

        return StoredDocumentFile.builder()
                .fileName(originalFileName)
                .fileType(fileType)
                .fileSize(multipartFile.getSize())
                .storagePath(toStoragePath(targetPath))
                .absolutePath(targetPath)
                .build();
    }

    /**
     * 尽量删除已保存文件，用于数据库写入失败时回滚本地文件副作用。
     *
     * @param storedDocumentFile 已保存文件信息
     */
    public void deleteQuietly(StoredDocumentFile storedDocumentFile) {
        if (storedDocumentFile == null || storedDocumentFile.getAbsolutePath() == null) {
            return;
        }
        try {
            Files.deleteIfExists(storedDocumentFile.getAbsolutePath());
        } catch (IOException ignored) {
            // 清理失败不覆盖原始业务异常，后续可接入日志或告警。
        }
    }

    /**
     * 静默删除指定路径，用于保存失败或业务回滚时清理文件。
     */
    private void deletePathQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 保存失败后的兜底清理，不能覆盖原始保存异常。
        }
    }

    /**
     * 获取归一化后的上传根目录，确保后续 startsWith 校验稳定可靠。
     */
    private Path getUploadRoot() {
        try {
            return Paths.get(appFileProperties.getUploadDir()).toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            throw new BusinessException("文件上传目录配置非法");
        }
    }

    /**
     * 校验文件非空。
     */
    private void validateNotEmpty(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty() || multipartFile.getSize() <= 0) {
            throw new BusinessException("文件不能为空");
        }
    }

    /**
     * 校验文件大小不超过业务配置限制。
     */
    private void validateFileSize(MultipartFile multipartFile) {
        long maxSize = appFileProperties.getMaxSize().toBytes();
        if (multipartFile.getSize() > maxSize) {
            throw new BusinessException("文件大小不能超过" + appFileProperties.getMaxSize());
        }
    }

    /**
     * 获取并校验对外展示的原始文件名，禁止路径穿越和客户端路径片段。
     */
    private String resolveOriginalFileName(MultipartFile multipartFile, String customFileName) {
        String sourceFileName = StringUtils.hasText(customFileName)
                ? customFileName.trim()
                : multipartFile.getOriginalFilename();
        if (!StringUtils.hasText(sourceFileName)) {
            throw new BusinessException("文件名不能为空");
        }

        String cleanedFileName = StringUtils.cleanPath(sourceFileName.trim());
        if (cleanedFileName.contains("..")
                || cleanedFileName.contains("/")
                || cleanedFileName.contains("\\")
                || cleanedFileName.length() > 255) {
            throw new BusinessException("文件名非法");
        }
        return cleanedFileName;
    }

    /**
     * 根据文件扩展名识别类型，当前第二周 P0 仅允许 txt 和 md。
     */
    private String resolveAndValidateFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new BusinessException("文件类型不能为空");
        }
        String fileType = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_FILE_TYPES.contains(fileType)) {
            throw new BusinessException("仅支持 txt、md 文件上传");
        }
        return fileType;
    }

    /**
     * 将本地路径转换为稳定的相对展示路径，避免向外暴露机器绝对路径。
     */
    private String toStoragePath(Path targetPath) {
        Path root = getUploadRoot();
        Path relativePath = root.relativize(targetPath.toAbsolutePath().normalize());
        String normalizedUploadDir = appFileProperties.getUploadDir().replace("\\", "/");
        return normalizedUploadDir + "/" + relativePath.toString().replace("\\", "/");
    }

    /**
     * 已保存文件的元信息。
     */
    @Data
    @Builder
    public static class StoredDocumentFile {

        /** 对外展示的原始文件名。 */
        private String fileName;

        /** 文件类型。 */
        private String fileType;

        /** 文件大小，单位字节。 */
        private Long fileSize;

        /** 写入 document.storage_path 的相对存储路径。 */
        private String storagePath;

        /** 本地绝对路径，仅用于失败清理。 */
        private Path absolutePath;
    }
}
