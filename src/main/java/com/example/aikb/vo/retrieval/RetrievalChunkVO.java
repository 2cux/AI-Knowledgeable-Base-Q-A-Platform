package com.example.aikb.vo.retrieval;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 单条检索命中结果，后续 QA 模块可直接作为上下文来源元数据使用。
 */
@Data
@Builder
@Schema(description = "检索命中的文档切片")
public class RetrievalChunkVO {

    @Schema(description = "chunk ID", example = "1")
    private Long chunkId;

    @Schema(description = "文档ID", example = "1")
    private Long documentId;

    @Schema(description = "知识库ID", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "chunk在文档内的顺序，从0开始", example = "0")
    private Integer chunkIndex;

    @Schema(description = "chunk文本内容")
    private String content;

    @Schema(description = "相似度分数，越大越相关", example = "0.87")
    private Double score;

    @Schema(description = "文档名称", example = "产品手册.pdf")
    private String documentName;
}
