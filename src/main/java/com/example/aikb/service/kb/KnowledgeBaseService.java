package com.example.aikb.service.kb;

import com.example.aikb.common.PageResult;
import com.example.aikb.dto.kb.KnowledgeBaseCreateRequest;
import com.example.aikb.dto.kb.KnowledgeBasePageRequest;
import com.example.aikb.dto.kb.KnowledgeBaseUpdateRequest;
import com.example.aikb.vo.kb.KnowledgeBaseVO;

/**
 * 知识库业务服务，提供知识库创建、分页查询和详情查询能力。
 */
public interface KnowledgeBaseService {

    /**
     * 创建当前用户的知识库。
     *
     * @param request 知识库创建请求参数
     * @return 创建后的知识库信息
     */
    KnowledgeBaseVO create(KnowledgeBaseCreateRequest request);

    /**
     * 分页查询当前用户的知识库列表。
     *
     * @param request 分页请求参数
     * @return 知识库分页结果
     */
    PageResult<KnowledgeBaseVO> page(KnowledgeBasePageRequest request);

    /**
     * 查询当前用户指定知识库详情。
     *
     * @param id 知识库 ID
     * @return 知识库详情
     */
    KnowledgeBaseVO getById(Long id);

    /**
     * 修改当前登录用户自己的知识库基础信息。
     *
     * @param id 知识库 ID
     * @param request 知识库修改请求参数
     * @return 修改后的知识库信息
     */
    KnowledgeBaseVO update(Long id, KnowledgeBaseUpdateRequest request);

    /**
     * 逻辑删除当前登录用户自己的知识库。
     *
     * @param id 知识库 ID
     */
    void delete(Long id);
}
