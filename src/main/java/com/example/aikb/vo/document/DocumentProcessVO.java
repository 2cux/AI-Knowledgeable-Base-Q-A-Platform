package com.example.aikb.vo.document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 文档处理结果展示信息。
 */
@Data
@Builder
@Schema(description = "文档处理结果展示信息")
public class DocumentProcessVO {

    /** 文档 ID。 */
    @Schema(description = "文档 ID", example = "1")
    private Long documentId;

    /** 知识库 ID。 */
    @Schema(description = "知识库 ID", example = "1")
    private Long knowledgeBaseId;

    /** 本次生成的切片数量。 */
    @Schema(description = "本次生成的切片数量", example = "12")
    private Integer chunkCount;

    /** 文档处理后的解析状态。 */
    @Schema(description = "文档处理后的解析状态", example = "CHUNKED")
    private String parseStatus;

    /** 本次处理任务 ID。 */
    @Schema(description = "本次处理任务 ID", example = "1")
    private Long taskId;

    /** 本次处理任务状态。 */
    @Schema(description = "本次处理任务状态", example = "SUCCESS")
    private String taskStatus;
}
