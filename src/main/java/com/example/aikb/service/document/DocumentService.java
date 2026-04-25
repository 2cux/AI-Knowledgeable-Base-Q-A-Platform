package com.example.aikb.service.document;

import com.example.aikb.common.PageResult;
import com.example.aikb.dto.document.DocumentFileUploadRequest;
import com.example.aikb.dto.document.DocumentListQuery;
import com.example.aikb.dto.document.DocumentUploadRequest;
import com.example.aikb.vo.document.DocumentDetailVO;
import com.example.aikb.vo.document.DocumentFileUploadVO;
import com.example.aikb.vo.document.DocumentListVO;
import com.example.aikb.vo.document.DocumentStatusVO;
import java.util.List;

/**
 * 文档服务接口。
 */
public interface DocumentService {

    DocumentDetailVO upload(DocumentUploadRequest request);

    DocumentFileUploadVO uploadFile(DocumentFileUploadRequest request);

    PageResult<DocumentListVO> page(DocumentListQuery query);

    List<DocumentListVO> listByKnowledgeBase(Long knowledgeBaseId);

    DocumentDetailVO getById(Long id);

    DocumentStatusVO getStatus(Long id);

    Long createParseTask(Long id);
}
