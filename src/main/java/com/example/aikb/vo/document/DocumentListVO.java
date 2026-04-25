package com.example.aikb.vo.document;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 文档列表项响应对象。
 */
@Data
@Builder
@Schema(description = "文档列表项响应对象")
public class DocumentListVO {

    @Schema(description = "文档ID", example = "1")
    private Long id;

    @Schema(description = "文档ID", example = "1")
    private Long documentId;

    @Schema(description = "知识库ID", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "原始文件名", example = "product-manual.pdf")
    private String fileName;

    @Schema(description = "文件类型", example = "pdf")
    private String fileType;

    @Schema(description = "文件大小，单位字节", example = "204800")
    private Long fileSize;

    @Schema(description = "解析状态", example = "UPLOADED")
    private String parseStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
