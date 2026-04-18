package com.example.aikb.service.retrieval.adapter;

import lombok.Data;

/**
 * 检索适配层内部候选结果，由文档切片和向量化元数据组装而来。
 */
@Data
public class RetrievalCandidate {

    private Long chunkId;
    private Long documentId;
    private Long knowledgeBaseId;
    private Integer chunkIndex;
    private String content;
    private String documentName;
    private String vectorId;
    private String embeddingModel;
    private Double score;
}
