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
     * Load chunks that have been embedded successfully in one knowledge base.
     * The MVP adapter scores these candidates in Java; a real vector store adapter should replace this query path.
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
                ce.embedding_model AS embeddingModel
            FROM chunk_embedding ce
            INNER JOIN document_chunk dc ON dc.id = ce.chunk_id
            INNER JOIN document d ON d.id = dc.document_id
            WHERE ce.knowledge_base_id = #{knowledgeBaseId}
              AND dc.knowledge_base_id = #{knowledgeBaseId}
              AND d.knowledge_base_id = #{knowledgeBaseId}
              AND ce.status = #{status}
            LIMIT #{limit}
            """)
    List<RetrievalCandidate> selectRetrievalCandidates(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("status") String status,
            @Param("limit") Integer limit);
}
