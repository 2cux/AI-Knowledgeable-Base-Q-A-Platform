package com.example.aikb.vo.document;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 单个切片向量化状态展示信息。
 */
@Data
@Builder
@Schema(description = "单个切片向量化状态展示信息")
public class ChunkEmbeddingStatusVO {

    /** 文档切片 ID。 */
    @Schema(description = "文档切片 ID", example = "1")
    private Long chunkId;

    /** 切片顺序。 */
    @Schema(description = "切片顺序", example = "0")
    private Integer chunkIndex;

    /** 向量化模型名称。 */
    @Schema(description = "向量化模型名称", example = "mock-embedding-v1")
    private String embeddingModel;

    /** 向量 ID。 */
    @Schema(description = "向量 ID", example = "mock-embedding-v1-1")
    private String vectorId;

    /** 向量化状态。 */
    @Schema(description = "向量化状态", example = "SUCCESS")
    private String status;

    /** 向量化失败原因。 */
    @Schema(description = "向量化失败原因")
    private String embeddingError;

    /** 向量化完成时间。 */
    @Schema(description = "向量化完成时间")
    private LocalDateTime embeddedAt;
}
