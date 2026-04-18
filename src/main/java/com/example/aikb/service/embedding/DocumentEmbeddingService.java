package com.example.aikb.service.embedding;

import com.example.aikb.dto.document.DocumentEmbeddingRequest;
import com.example.aikb.vo.document.DocumentEmbeddingStatusVO;
import com.example.aikb.vo.document.DocumentEmbeddingVO;

/**
 * 文档向量化服务，负责读取文档 chunk、调用向量化客户端并维护同步状态。
 */
public interface DocumentEmbeddingService {

    /**
     * 对当前用户可访问的指定文档执行向量化。
     *
     * @param documentId 文档 ID
     * @param request 文档向量化请求参数
     * @return 文档向量化执行结果
     */
    DocumentEmbeddingVO embedDocument(Long documentId, DocumentEmbeddingRequest request);

    /**
     * 查询当前用户可访问的指定文档向量化状态。
     *
     * @param documentId 文档 ID
     * @return 文档向量化状态汇总
     */
    DocumentEmbeddingStatusVO getEmbeddingStatus(Long documentId);
}
