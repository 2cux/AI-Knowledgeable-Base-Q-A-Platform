package com.example.aikb.vo.document;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * 文档切片展示信息。
 */
@Data
@Builder
@Schema(description = "文档切片展示信息")
public class DocumentChunkVO {

    /** 切片 ID。 */
    @Schema(description = "切片 ID", example = "1")
    private Long id;

    /** 文档 ID。 */
    @Schema(description = "文档 ID", example = "1")
    private Long documentId;

    /** 知识库 ID。 */
    @Schema(description = "知识库 ID", example = "1")
    private Long knowledgeBaseId;

    /** 切片顺序。 */
    @Schema(description = "切片顺序", example = "0")
    private Integer chunkIndex;

    /** 切片文本内容。 */
    @Schema(description = "切片文本内容")
    private String content;

    /** MVP 阶段使用字符数近似 token 数。 */
    @Schema(description = "MVP 阶段使用字符数近似 token 数", example = "500")
    private Integer tokenCount;

    /** 创建时间。 */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
