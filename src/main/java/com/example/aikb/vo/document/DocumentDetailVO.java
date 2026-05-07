package com.example.aikb.vo.document;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 文档详情响应对象。
 */
@Data
@Builder
@Schema(description = "文档详情响应对象")
public class DocumentDetailVO {

    @Schema(description = "文档ID", example = "1")
    private Long id;

    @Schema(description = "文档ID", example = "1")
    private Long documentId;

    @Schema(description = "知识库ID", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "原始文件名", example = "product-faq.md")
    private String fileName;

    @Schema(description = "Original file name", example = "product-faq.md")
    private String originalFileName;

    @Schema(description = "文件类型，当前仅支持 txt、md", example = "md")
    private String fileType;

    @Schema(description = "文件大小，单位字节", example = "204800")
    private Long fileSize;

    @Schema(description = "存储路径", example = "metadata/1/product-faq.md")
    private String storagePath;

    @Schema(description = "解析状态；文件上传成功后初始为 NOT_STARTED，需要调用 process 接口后才会进入解析流程", example = "NOT_STARTED")
    private String parseStatus;

    @Schema(description = "Chunk count", example = "12")
    private Integer chunkCount;

    @Schema(description = "Embedding status", example = "SUCCESS")
    private String embeddingStatus;

    @Schema(description = "Embedded chunk count", example = "12")
    private Integer embeddedChunkCount;

    @Schema(description = "Latest task type", example = "DOCUMENT_PROCESS")
    private String latestTaskType;

    @Schema(description = "Latest task status", example = "SUCCESS")
    private String latestTaskStatus;

    @Schema(description = "Latest error message")
    private String latestErrorMessage;

    @Schema(description = "Whether force process is allowed", example = "true")
    private Boolean canReprocess;

    @Schema(description = "Whether embedding is allowed", example = "true")
    private Boolean canReembed;

    @Schema(description = "上传人用户ID", example = "1")
    private Long createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
