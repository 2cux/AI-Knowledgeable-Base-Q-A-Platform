package com.example.aikb.service.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.aikb.exception.BusinessException;
import org.junit.jupiter.api.Test;

class DocumentFileTypeUtilsTest {

    @Test
    void shouldNormalizeSupportedFileTypes() {
        assertThat(DocumentFileTypeUtils.normalizeAndValidateFileType("txt")).isEqualTo("txt");
        assertThat(DocumentFileTypeUtils.normalizeAndValidateFileType("MD")).isEqualTo("md");
        assertThat(DocumentFileTypeUtils.normalizeAndValidateFileType(" Md ")).isEqualTo("md");
        assertThat(DocumentFileTypeUtils.normalizeAndValidateFileType("PDF")).isEqualTo("pdf");
        assertThat(DocumentFileTypeUtils.normalizeAndValidateFileType("Docx")).isEqualTo("docx");
    }

    @Test
    void shouldResolveSupportedExtensionsCaseInsensitively() {
        assertThat(DocumentFileTypeUtils.resolveAndValidateExtension("note.TXT")).isEqualTo("txt");
        assertThat(DocumentFileTypeUtils.resolveAndValidateExtension("a.b.Md")).isEqualTo("md");
        assertThat(DocumentFileTypeUtils.resolveAndValidateExtension("report.PDF")).isEqualTo("pdf");
        assertThat(DocumentFileTypeUtils.resolveAndValidateExtension("memo.Docx")).isEqualTo("docx");
    }

    @Test
    void shouldRejectUnsupportedExtensions() {
        assertThatThrownBy(() -> DocumentFileTypeUtils.resolveAndValidateExtension("manual.exe"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(DocumentFileTypeUtils.SUPPORTED_FILE_MESSAGE);
        assertThatThrownBy(() -> DocumentFileTypeUtils.resolveAndValidateExtension("archive.zip"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(DocumentFileTypeUtils.SUPPORTED_FILE_MESSAGE);
        assertThatThrownBy(() -> DocumentFileTypeUtils.resolveAndValidateExtension("image.png"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(DocumentFileTypeUtils.SUPPORTED_FILE_MESSAGE);
        assertThatThrownBy(() -> DocumentFileTypeUtils.resolveAndValidateExtension("archive.tar.gz"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(DocumentFileTypeUtils.SUPPORTED_FILE_MESSAGE);
    }

    @Test
    void shouldRejectBlankMissingOrUnsafeFileNames() {
        assertThatThrownBy(() -> DocumentFileTypeUtils.resolveAndValidateExtension(" "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(DocumentFileTypeUtils.SUPPORTED_FILE_MESSAGE);
        assertThatThrownBy(() -> DocumentFileTypeUtils.resolveAndValidateExtension("README"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(DocumentFileTypeUtils.SUPPORTED_FILE_MESSAGE);
        assertThatThrownBy(() -> DocumentFileTypeUtils.resolveAndValidateExtension("../secret.txt"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(DocumentFileTypeUtils.SUPPORTED_FILE_MESSAGE);
    }

    @Test
    void shouldValidateMetadataTypeAgainstFileNameExtension() {
        assertThat(DocumentFileTypeUtils.validateMetadataType("faq.TXT", "txt")).isEqualTo("txt");
        assertThat(DocumentFileTypeUtils.validateMetadataType("faq.md", "MD")).isEqualTo("md");
        assertThat(DocumentFileTypeUtils.validateMetadataType("report.pdf", "PDF")).isEqualTo("pdf");
        assertThat(DocumentFileTypeUtils.validateMetadataType("memo.docx", "docx")).isEqualTo("docx");

        assertThatThrownBy(() -> DocumentFileTypeUtils.validateMetadataType("faq.txt", "md"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件名扩展名与 fileType 不一致");
        assertThatThrownBy(() -> DocumentFileTypeUtils.validateMetadataType("faq.md", "exe"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(DocumentFileTypeUtils.SUPPORTED_FILE_MESSAGE);
    }

    @Test
    void shouldUseParseMessageForUnsupportedProcessTypes() {
        DocumentFileTypeUtils.validateProcessSupported("TXT");
        DocumentFileTypeUtils.validateProcessSupported("PDF");
        DocumentFileTypeUtils.validateProcessSupported("DOCX");

        assertThatThrownBy(() -> DocumentFileTypeUtils.validateProcessSupported("exe"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(DocumentFileTypeUtils.SUPPORTED_PARSE_MESSAGE);
    }

    @Test
    void isSupportedShouldReturnTrueForNewTypes() {
        assertThat(DocumentFileTypeUtils.isSupported("txt")).isTrue();
        assertThat(DocumentFileTypeUtils.isSupported("md")).isTrue();
        assertThat(DocumentFileTypeUtils.isSupported("pdf")).isTrue();
        assertThat(DocumentFileTypeUtils.isSupported("docx")).isTrue();
        assertThat(DocumentFileTypeUtils.isSupported("exe")).isFalse();
        assertThat(DocumentFileTypeUtils.isSupported("zip")).isFalse();
    }
}
