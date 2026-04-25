package com.example.aikb.vo.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 文档状态概览响应对象。
 */
@Data
@Builder
@Schema(description = "文档状态概览响应对象")
public class DocumentStatusVO {

    @Schema(description = "文档ID", example = "1")
    private Long documentId;

    @Schema(description = "知识库ID", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "解析状态", example = "CHUNKED")
    private String parseStatus;

    @Schema(description = "当前切片数量", example = "12")
    private Integer chunkCount;

    @Schema(description = "向量化总数", example = "12")
    private Integer embeddingTotal;

    @Schema(description = "向量化成功数", example = "10")
    private Integer embeddingSuccessCount;

    @Schema(description = "向量化失败数", example = "2")
    private Integer embeddingFailedCount;

    @Schema(description = "最近任务状态", example = "SUCCESS")
    private String latestTaskStatus;

    @Schema(description = "最近任务错误信息")
    private String latestErrorMessage;
}
