package com.example.aikb.dto.document;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文档上传请求参数，MVP 阶段仅保存文件元数据。
 */
@Data
@Schema(description = "文档上传请求参数")
public class DocumentUploadRequest {

    /** 文档所属知识库 ID。 */
    @Schema(description = "文档所属知识库 ID", example = "1")
    @NotNull(message = "知识库ID不能为空")
    private Long knowledgeBaseId;

    /** 原始文件名称。 */
    @Schema(description = "原始文件名称", example = "产品手册.pdf")
    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名不能超过255个字符")
    private String fileName;

    /** 文件类型，例如 pdf、docx、txt。 */
    @Schema(description = "文件类型", example = "pdf")
    @NotBlank(message = "文件类型不能为空")
    @Size(max = 32, message = "文件类型不能超过32个字符")
    private String fileType;

    /** 文件大小，单位为字节。 */
    @Schema(description = "文件大小，单位为字节", example = "204800")
    @NotNull(message = "文件大小不能为空")
    @Min(value = 0, message = "文件大小不能小于0")
    private Long fileSize;

    /** 文件存储路径，不传时由服务端生成占位路径。 */
    @Schema(description = "文件存储路径，不传时由服务端生成占位路径", example = "kb/1/产品手册.pdf")
    @Size(max = 500, message = "文件存储路径不能超过500个字符")
    private String storagePath;
}
