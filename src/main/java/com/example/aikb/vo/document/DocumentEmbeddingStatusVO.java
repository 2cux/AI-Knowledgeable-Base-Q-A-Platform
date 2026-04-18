package com.example.aikb.vo.document;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 文档向量化状态汇总展示信息。
 */
@Data
@Builder
@Schema(description = "文档向量化状态汇总展示信息")
public class DocumentEmbeddingStatusVO {

    /** 文档 ID。 */
    @Schema(description = "文档 ID", example = "1")
    private Long documentId;

    /** 知识库 ID。 */
    @Schema(description = "知识库 ID", example = "1")
    private Long knowledgeBaseId;

    /** 文档 chunk 总数。 */
    @Schema(description = "文档 chunk 总数", example = "10")
    private Integer totalChunks;

    /** 已成功向量化数量。 */
    @Schema(description = "已成功向量化数量", example = "8")
    private Integer successCount;

    /** 向量化失败数量。 */
    @Schema(description = "向量化失败数量", example = "1")
    private Integer failedCount;

    /** 尚未向量化数量。 */
    @Schema(description = "尚未向量化数量", example = "1")
    private Integer pendingCount;

    /** 当前文档整体向量化状态。 */
    @Schema(description = "当前文档整体向量化状态", example = "PARTIAL")
    private String status;

    /** 每个 chunk 的向量化状态。 */
    @Schema(description = "每个 chunk 的向量化状态")
    private List<ChunkEmbeddingStatusVO> chunks;
}
