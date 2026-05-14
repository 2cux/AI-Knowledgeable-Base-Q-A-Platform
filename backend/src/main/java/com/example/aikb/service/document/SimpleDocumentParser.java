package com.example.aikb.service.document;

import com.example.aikb.entity.Document;
import com.example.aikb.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文档解析器：支持 txt、md、pdf、docx 文件解析。
 * txt/md 按 UTF-8 读取纯文本，pdf 使用 PDFBox 提取文本，docx 使用 POI 提取文本。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SimpleDocumentParser {

    private static final String METADATA_STORAGE_PREFIX = "metadata/";

    private final LocalDocumentStorage localDocumentStorage;

    /**
     * 获取文档纯文本内容。
     *
     * @param document    文档实体
     * @param textContent 请求直接传入的纯文本，主要用于旧元数据文档调试
     * @return 文档纯文本
     */
    public String parse(Document document, String textContent) {
        validateDocument(document);

        if (hasRealStoragePath(document)) {
            return parseByFileType(document);
        }

        if (StringUtils.hasText(textContent)) {
            return textContent.trim();
        }
        throw new BusinessException("文档没有可解析的真实文件，请先通过upload-file上传文件");
    }

    /**
     * 校验文档类型是否属于支持范围。
     */
    private void validateDocument(Document document) {
        if (document == null) {
            throw new BusinessException(40400, "文档不存在");
        }
        DocumentFileTypeUtils.validateProcessSupported(document.getFileType());
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
     * 按文件类型分发到具体的解析实现。
     */
    private String parseByFileType(Document document) {
        String fileType = document.getFileType().toLowerCase(Locale.ROOT);
        Path path = resolveDocumentPath(document);
        return switch (fileType) {
            case "txt", "md" -> readTextFile(path, document);
            case "pdf" -> parsePdf(path, document);
            case "docx" -> parseDocx(path, document);
            default -> throw new BusinessException("不支持的文档类型：" + fileType);
        };
    }

    /**
     * 解析真实上传文件路径并进行安全检查。
     */
    private Path resolveDocumentPath(Document document) {
        Path path = localDocumentStorage.resolveStoredPath(document.getStoragePath());
        if (!Files.exists(path)) {
            throw new BusinessException("文件不存在或已被删除，请重新上传");
        }
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new BusinessException("文档文件不可读");
        }
        return path;
    }

    /**
     * 按 UTF-8 读取 txt/md 文件内容。
     */
    private String readTextFile(Path path, Document document) {
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

    /**
     * 使用 Apache PDFBox 提取 PDF 文本内容。
     * 不解析图片，不做 OCR，不处理加密 PDF。
     */
    private String parsePdf(Path path, Document document) {
        try {
            PDDocument pdfDocument = Loader.loadPDF(path.toFile());
            try {
                if (pdfDocument.isEncrypted()) {
                    throw new BusinessException("PDF 文件已加密，暂不支持解析加密 PDF");
                }

                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(pdfDocument);

                if (!StringUtils.hasText(text)) {
                    throw new BusinessException("PDF 未提取到文本，可能是扫描件或加密文件，暂不支持 OCR 解析");
                }
                return cleanExtractedText(text);
            } finally {
                pdfDocument.close();
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            String message = ex.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("encrypt")) {
                throw new BusinessException("PDF 文件已加密，暂不支持解析加密 PDF");
            }
            log.warn("Read PDF file failed, documentId={}, storagePath={}",
                    document.getId(), document.getStoragePath(), ex);
            throw new BusinessException("PDF 文件解析失败，文件可能已损坏");
        }
    }

    /**
     * 使用 Apache POI 提取 DOCX 文本内容，包括段落和表格。
     * 不支持 .doc 老格式。
     */
    private String parseDocx(Path path, Document document) {
        try (InputStream is = Files.newInputStream(path);
             XWPFDocument docxDocument = new XWPFDocument(is)) {

            XWPFWordExtractor extractor = new XWPFWordExtractor(docxDocument);
            String text = extractor.getText();

            if (!StringUtils.hasText(text)) {
                throw new BusinessException("DOCX 文档内容为空，无法生成切片");
            }
            return cleanExtractedText(text);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            log.warn("Read DOCX file failed, documentId={}, storagePath={}",
                    document.getId(), document.getStoragePath(), ex);
            throw new BusinessException("DOCX 文件解析失败，文件可能已损坏");
        }
    }

    /**
     * 对 PDF/DOCX 提取的文本做统一清洗。
     * 1. 去除不可见控制字符（保留 \\n、\\r、\\t）
     * 2. trim
     * 3. 压缩 3+ 连续空行为 1 个空行
     * 4. 压缩多个连续空格为单个空格
     */
    static String cleanExtractedText(String text) {
        if (text == null) {
            return "";
        }
        // 去除控制字符 (保留 \n \r \t)
        String cleaned = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        cleaned = cleaned.trim();
        // 压缩 3 个以上连续换行为 1 个空行
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");
        // 压缩多个空格/tab 为单个空格
        cleaned = cleaned.replaceAll("[ \\t]+", " ");
        return cleaned.trim();
    }
}
