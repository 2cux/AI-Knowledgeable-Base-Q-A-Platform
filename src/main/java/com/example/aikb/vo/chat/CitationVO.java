package com.example.aikb.vo.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "答案引用来源")
public class CitationVO {

    @Schema(description = "切片ID", example = "1")
    private Long chunkId;

    @Schema(description = "文档ID", example = "1")
    private Long documentId;

    @Schema(description = "知识库ID", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "切片在文档内的序号", example = "0")
    private Integer chunkIndex;

    @Schema(description = "文档名称", example = "manual.pdf")
    private String documentName;

    @Schema(description = "检索相关性分数", example = "0.87")
    private Double score;

    @Schema(description = "引用内容摘要")
    private String contentSnippet;
}
