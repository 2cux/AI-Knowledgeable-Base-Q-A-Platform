package com.example.aikb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 切片向量化状态实体，记录每个 chunk 同步到向量服务后的状态和向量标识。
 */
@Data
@TableName("chunk_embedding")
@Schema(description = "切片向量化状态实体")
public class ChunkEmbedding {

    /** 主键 ID。 */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档 ID，便于按文档聚合查询向量化状态。 */
    @Schema(description = "文档 ID", example = "1")
    @TableField("document_id")
    private Long documentId;

    /** 知识库 ID，便于后续按知识库过滤检索。 */
    @Schema(description = "知识库 ID", example = "1")
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    /** 文档切片 ID。 */
    @Schema(description = "文档切片 ID", example = "1")
    @TableField("chunk_id")
    private Long chunkId;

    /** 向量化模型名称。 */
    @Schema(description = "向量化模型名称", example = "mock-embedding-v1")
    @TableField("embedding_model")
    private String embeddingModel;

    /** 向量库中的向量 ID，MVP 阶段由本地向量实现生成。 */
    @Schema(description = "向量库中的向量 ID", example = "local-hash-embedding-v1-1")
    @TableField("vector_id")
    private String vectorId;

    /** 向量内容 JSON，供真实检索模块接入前后复用。 */
    @Schema(description = "向量内容 JSON")
    @TableField("vector_json")
    private String vectorJson;

    /** 向量化状态：PENDING、PROCESSING、SUCCESS、FAILED。 */
    @Schema(description = "向量化状态", example = "SUCCESS")
    private String status;

    /** 向量化失败原因。 */
    @Schema(description = "向量化失败原因")
    @TableField("embedding_error")
    private String embeddingError;

    /** 向量化完成时间。 */
    @Schema(description = "向量化完成时间")
    @TableField("embedded_at")
    private LocalDateTime embeddedAt;

    /** 创建时间。 */
    @Schema(description = "创建时间")
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间。 */
    @Schema(description = "更新时间")
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
