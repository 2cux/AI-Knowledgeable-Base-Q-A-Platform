package com.example.aikb.vo.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 文档向量化执行结果展示信息。
 */
@Data
@Builder
@Schema(description = "文档向量化执行结果展示信息")
public class DocumentEmbeddingVO {

    /** 文档 ID。 */
    @Schema(description = "文档 ID", example = "1")
    private Long documentId;

    /** 知识库 ID。 */
    @Schema(description = "知识库 ID", example = "1")
    private Long knowledgeBaseId;

    /** 本次处理的 chunk 总数。 */
    @Schema(description = "本次处理的 chunk 总数", example = "10")
    private Integer total;

    /** 向量化成功数量。 */
    @Schema(description = "向量化成功数量", example = "10")
    private Integer successCount;

    /** 向量化失败数量。 */
    @Schema(description = "向量化失败数量", example = "0")
    private Integer failedCount;

    /** 本次使用的向量化模型。 */
    @Schema(description = "本次使用的向量化模型", example = "local-hash-embedding-v1")
    private String embeddingModel;

    /** 本次向量化任务 ID。 */
    @Schema(description = "本次向量化任务 ID", example = "1")
    private Long taskId;

    /** 本次向量化任务状态。 */
    @Schema(description = "本次向量化任务状态", example = "SUCCESS")
    private String taskStatus;
}
