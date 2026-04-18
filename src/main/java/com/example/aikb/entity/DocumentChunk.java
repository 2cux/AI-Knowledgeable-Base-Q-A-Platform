package com.example.aikb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 文档切片实体，保存文档解析后的文本片段，为后续 embedding 和检索提供基础数据。
 */
@Data
@TableName("document_chunk")
@Schema(description = "文档切片实体")
public class DocumentChunk {

    /** 切片主键 ID。 */
    @Schema(description = "切片主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 切片所属文档 ID。 */
    @Schema(description = "切片所属文档 ID", example = "1")
    @TableField("document_id")
    private Long documentId;

    /** 切片所属知识库 ID。 */
    @Schema(description = "切片所属知识库 ID", example = "1")
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    /** 切片在文档内的顺序，从 0 开始。 */
    @Schema(description = "切片在文档内的顺序，从 0 开始", example = "0")
    @TableField("chunk_index")
    private Integer chunkIndex;

    /** 切片文本内容。 */
    @Schema(description = "切片文本内容")
    private String content;

    /** MVP 阶段使用字符数近似 token 数。 */
    @Schema(description = "MVP 阶段使用字符数近似 token 数", example = "500")
    @TableField("token_count")
    private Integer tokenCount;

    /** 创建时间。 */
    @Schema(description = "创建时间")
    @TableField("created_at")
    private LocalDateTime createdAt;
}
