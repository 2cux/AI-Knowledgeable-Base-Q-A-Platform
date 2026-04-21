package com.example.aikb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aikb.entity.ChunkEmbedding;
import com.example.aikb.service.retrieval.adapter.RetrievalCandidate;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 切片向量化状态数据访问接口，提供 chunk_embedding 表基础 CRUD 能力。
 */
public interface ChunkEmbeddingMapper extends BaseMapper<ChunkEmbedding> {

    /**
     * 查询指定知识库内已向量化成功的 chunk，供 MVP 检索适配器在应用层计算相似度。
     * 后续接入真实向量库时，可替换该查询路径。
     */
    @Select("""
            SELECT
                dc.id AS chunkId,
                dc.document_id AS documentId,
                dc.knowledge_base_id AS knowledgeBaseId,
                dc.chunk_index AS chunkIndex,
                dc.content AS content,
                d.file_name AS documentName,
                ce.vector_id AS vectorId,
                ce.embedding_model AS embeddingModel,
                ce.vector_json AS vectorJson
            FROM chunk_embedding ce
            INNER JOIN document_chunk dc ON dc.id = ce.chunk_id
            INNER JOIN document d ON d.id = dc.document_id
            WHERE ce.knowledge_base_id = #{knowledgeBaseId}
              AND dc.knowledge_base_id = #{knowledgeBaseId}
              AND d.knowledge_base_id = #{knowledgeBaseId}
              AND ce.status = #{status}
              AND ce.embedding_model = #{embeddingModel}
              AND ce.vector_json IS NOT NULL
            ORDER BY ce.id ASC
            LIMIT #{limit}
            """)
    List<RetrievalCandidate> selectRetrievalCandidates(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("status") String status,
            @Param("embeddingModel") String embeddingModel,
            @Param("limit") Integer limit);
}
