package com.example.aikb.service.retrieval;

import com.example.aikb.dto.retrieval.RetrievalSearchRequest;
import com.example.aikb.vo.retrieval.RetrievalSearchVO;

/**
 * 检索服务边界。后续 QA 生成模块应依赖该服务获取上下文，而不是直接查询 chunk。
 */
public interface RetrievalService {

    /**
     * 在当前用户可访问的知识库中检索最相关的 chunk。
     *
     * @param request 检索请求参数
     * @return topK 相关 chunk
     */
    RetrievalSearchVO search(RetrievalSearchRequest request);
}
