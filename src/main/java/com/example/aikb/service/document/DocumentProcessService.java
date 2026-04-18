package com.example.aikb.service.document;

import com.example.aikb.dto.document.DocumentProcessRequest;
import com.example.aikb.vo.document.DocumentChunkVO;
import com.example.aikb.vo.document.DocumentProcessVO;
import java.util.List;

/**
 * 文档处理服务，负责解析纯文本、切片入库和切片查询。
 */
public interface DocumentProcessService {

    /**
     * 对当前用户可访问的指定文档执行文本切片入库。
     *
     * @param documentId 文档 ID
     * @param request 文档处理请求参数
     * @return 文档处理结果
     */
    DocumentProcessVO process(Long documentId, DocumentProcessRequest request);

    /**
     * 查询当前用户可访问的指定文档切片列表。
     *
     * @param documentId 文档 ID
     * @return 文档切片列表
     */
    List<DocumentChunkVO> listChunks(Long documentId);
}
