package com.example.aikb.dto.document;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 真实文件上传请求参数，使用 multipart/form-data 承载知识库 ID 和文件内容。
 */
@Data
@Schema(description = "真实文件上传请求参数")
public class DocumentFileUploadRequest {

    /** 文档所属知识库 ID。 */
    @Schema(description = "文档所属知识库 ID", example = "1")
    @NotNull(message = "知识库ID不能为空")
    @Positive(message = "知识库ID必须大于0")
    private Long knowledgeBaseId;

    /** 上传的真实文件，仅支持 txt 和 md。 */
    @Schema(description = "上传的真实文件，仅支持 txt 和 md")
    @NotNull(message = "文件不能为空")
    private MultipartFile file;

    /** 可选自定义文件名，不传时使用 multipart 原始文件名。 */
    @Schema(description = "可选自定义文件名，不传时使用原始文件名", example = "产品FAQ.md")
    @Size(max = 255, message = "文件名不能超过255个字符")
    private String fileName;
}
