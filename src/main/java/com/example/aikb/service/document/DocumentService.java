package com.example.aikb.service.document;

import com.example.aikb.common.PageResult;
import com.example.aikb.dto.document.DocumentFileUploadRequest;
import com.example.aikb.dto.document.DocumentListQuery;
import com.example.aikb.dto.document.DocumentUploadRequest;
import com.example.aikb.vo.document.DocumentDetailVO;
import com.example.aikb.vo.document.DocumentFileUploadVO;
import com.example.aikb.vo.document.DocumentListVO;

/**
 * 文档业务服务，提供文档元数据上传、分页查询、详情查询和解析任务创建能力。
 */
public interface DocumentService {

    /**
     * 上传文档元数据，并绑定到当前用户拥有的知识库。
     *
     * @param request 文档上传请求参数
     * @return 文档详情
     */
    DocumentDetailVO upload(DocumentUploadRequest request);

    /**
     * 上传真实文件，保存到后端受控目录后再写入文档记录。
     *
     * @param request 真实文件上传请求参数
     * @return 文件上传结果
     */
    DocumentFileUploadVO uploadFile(DocumentFileUploadRequest request);

    /**
     * 分页查询当前用户可访问的文档列表，可按知识库筛选。
     *
     * @param query 文档分页查询参数
     * @return 文档分页结果
     */
    PageResult<DocumentListVO> page(DocumentListQuery query);

    /**
     * 查询当前用户可访问的文档详情。
     *
     * @param id 文档 ID
     * @return 文档详情
     */
    DocumentDetailVO getById(Long id);

    /**
     * 为当前用户可访问的文档创建解析任务记录。
     *
     * @param id 文档 ID
     * @return 解析任务 ID
     */
    Long createParseTask(Long id);
}
