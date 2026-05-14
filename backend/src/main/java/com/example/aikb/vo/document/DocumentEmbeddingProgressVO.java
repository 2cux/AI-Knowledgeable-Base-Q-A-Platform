package com.example.aikb.vo.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "文档向量化进度展示信息")
public class DocumentEmbeddingProgressVO {

    @Schema(description = "文档 ID", example = "1")
    private Long documentId;

    @Schema(description = "向量化状态: NOT_STARTED / PROCESSING / SUCCESS / FAILED / PARTIAL_SUCCESS", example = "PROCESSING")
    private String status;

    @Schema(description = "Chunk 总数", example = "200")
    private Integer totalChunks;

    @Schema(description = "已向量化的 chunk 数量", example = "56")
    private Integer embeddedChunks;

    @Schema(description = "进度百分比 0～100", example = "28")
    private Integer progress;

    @Schema(description = "错误信息")
    private String errorMessage;
}
