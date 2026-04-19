package com.example.aikb.vo.retrieval;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 检索响应结果。返回结构以 chunk 为核心，便于问答模块组装上下文。
 */
@Data
@Builder
@Schema(description = "检索结果")
public class RetrievalSearchVO {

    @Schema(description = "知识库ID", example = "1")
    private Long knowledgeBaseId;

    @Schema(description = "原始问题", example = "如何重置管理员密码？")
    private String question;

    @Schema(description = "实际使用的topK", example = "5")
    private Integer topK;

    @Schema(description = "命中的chunk数量", example = "3")
    private Integer total;

    @Schema(description = "命中的文档切片")
    private List<RetrievalChunkVO> chunks;
}
