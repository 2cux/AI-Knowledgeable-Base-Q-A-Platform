package com.example.aikb.service.document.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aikb.dto.document.DocumentProcessRequest;
import com.example.aikb.entity.Document;
import com.example.aikb.entity.DocumentChunk;
import com.example.aikb.entity.KnowledgeBase;
import com.example.aikb.entity.TaskRecord;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.DocumentChunkMapper;
import com.example.aikb.mapper.DocumentMapper;
import com.example.aikb.mapper.KnowledgeBaseMapper;
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.document.DocumentProcessService;
import com.example.aikb.service.document.SimpleDocumentParser;
import com.example.aikb.service.document.TextSplitter;
import com.example.aikb.service.task.TaskRecordService;
import com.example.aikb.vo.document.DocumentChunkVO;
import com.example.aikb.vo.document.DocumentProcessVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 文档处理服务实现类，负责将文档纯文本切片并写入 document_chunk 表。
 */
@Service
@RequiredArgsConstructor
public class DocumentProcessServiceImpl implements DocumentProcessService {

    private static final String PARSE_STATUS_CHUNKING = "CHUNKING";
    private static final String PARSE_STATUS_CHUNKED = "CHUNKED";
    private static final String TASK_STATUS_SUCCESS = "SUCCESS";

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final SimpleDocumentParser simpleDocumentParser;
    private final TextSplitter textSplitter;
    private final TaskRecordService taskRecordService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 对当前用户可访问的指定文档执行文本切片入库。
     *
     * @param documentId 文档 ID
     * @param request 文档处理请求参数
     * @return 文档处理结果
     */
    @Override
    public DocumentProcessVO process(Long documentId, DocumentProcessRequest request) {
        DocumentProcessRequest safeRequest = normalizeRequest(request);
        if (safeRequest.getOverlap() >= safeRequest.getChunkSize()) {
            throw new BusinessException("overlap必须小于chunkSize");
        }

        Long userId = CurrentUser.getUserId();
        Document document = getOwnDocument(documentId, userId);
        TaskRecord taskRecord = taskRecordService.createDocumentProcessTask(document, userId);
        taskRecordService.markProcessing(taskRecord.getId());

        try {
            DocumentProcessVO result = transactionTemplate.execute(status ->
                    processInTransaction(document, safeRequest, taskRecord.getId()));
            taskRecordService.markSuccess(taskRecord.getId());
            result.setTaskStatus(TASK_STATUS_SUCCESS);
            return result;
        } catch (RuntimeException ex) {
            taskRecordService.markFailed(taskRecord.getId(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * 在事务内执行文档状态更新、真实解析和切片重建，失败时整体回滚。
     */
    private DocumentProcessVO processInTransaction(Document document, DocumentProcessRequest safeRequest, Long taskId) {
        Document latestDocument = documentMapper.selectById(document.getId());
        if (latestDocument == null) {
            throw new BusinessException(40400, "文档不存在");
        }
        if (PARSE_STATUS_CHUNKING.equals(latestDocument.getParseStatus())) {
            throw new BusinessException("文档正在切片处理中，请稍后再试");
        }

        updateDocumentStatus(latestDocument.getId(), PARSE_STATUS_CHUNKING);
        String text = simpleDocumentParser.parse(latestDocument, safeRequest.getTextContent());
        List<String> chunks = textSplitter.split(text, safeRequest.getChunkSize(), safeRequest.getOverlap());
        if (chunks.isEmpty()) {
            throw new BusinessException("文档内容为空，无法生成切片");
        }

        rebuildChunks(latestDocument, chunks);
        updateDocumentStatus(latestDocument.getId(), PARSE_STATUS_CHUNKED);

        return DocumentProcessVO.builder()
                .documentId(latestDocument.getId())
                .knowledgeBaseId(latestDocument.getKnowledgeBaseId())
                .chunkCount(chunks.size())
                .parseStatus(PARSE_STATUS_CHUNKED)
                .taskId(taskId)
                .build();
    }

    /**
     * 归一化处理请求，避免客户端显式传 null 时覆盖 DTO 默认值。
     *
     * @param request 原始处理请求
     * @return 带默认值的处理请求
     */
    private DocumentProcessRequest normalizeRequest(DocumentProcessRequest request) {
        DocumentProcessRequest safeRequest = request == null ? new DocumentProcessRequest() : request;
        if (safeRequest.getChunkSize() == null) {
            safeRequest.setChunkSize(500);
        }
        if (safeRequest.getOverlap() == null) {
            safeRequest.setOverlap(50);
        }
        return safeRequest;
    }

    /**
     * 查询当前用户可访问的指定文档切片列表。
     *
     * @param documentId 文档 ID
     * @return 文档切片列表
     */
    @Override
    public List<DocumentChunkVO> listChunks(Long documentId) {
        Long userId = CurrentUser.getUserId();
        Document document = getOwnDocument(documentId, userId);
        return documentChunkMapper.selectList(new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, document.getId())
                        .orderByAsc(DocumentChunk::getChunkIndex))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 删除旧切片并重建新切片，保证重复处理文档时数据不会重复。
     *
     * @param document 文档实体
     * @param chunks 切片文本列表
     */
    private void rebuildChunks(Document document, List<String> chunks) {
        documentChunkMapper.delete(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, document.getId()));

        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i);
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(document.getId());
            chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
            chunk.setChunkIndex(i);
            chunk.setContent(content);
            chunk.setTokenCount(content.length());
            documentChunkMapper.insert(chunk);
        }
    }

    /**
     * 查询并校验文档是否属于当前用户自己的知识库。
     *
     * @param documentId 文档 ID
     * @param userId 当前用户 ID
     * @return 文档实体
     */
    private Document getOwnDocument(Long documentId, Long userId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(40400, "文档不存在");
        }

        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, document.getKnowledgeBaseId())
                .eq(KnowledgeBase::getOwnerId, userId)
                .eq(KnowledgeBase::getStatus, 1)
                .last("LIMIT 1"));
        if (knowledgeBase == null) {
            throw new BusinessException(40400, "文档不存在");
        }
        return document;
    }

    /**
     * 更新文档解析状态。
     *
     * @param documentId 文档 ID
     * @param parseStatus 解析状态
     */
    private void updateDocumentStatus(Long documentId, String parseStatus) {
        Document update = new Document();
        update.setId(documentId);
        update.setParseStatus(parseStatus);
        documentMapper.updateById(update);
    }

    /**
     * 将切片实体转换为接口展示对象。
     *
     * @param chunk 切片实体
     * @return 切片展示对象
     */
    private DocumentChunkVO toVO(DocumentChunk chunk) {
        return DocumentChunkVO.builder()
                .id(chunk.getId())
                .documentId(chunk.getDocumentId())
                .knowledgeBaseId(chunk.getKnowledgeBaseId())
                .chunkIndex(chunk.getChunkIndex())
                .content(chunk.getContent())
                .tokenCount(chunk.getTokenCount())
                .createdAt(chunk.getCreatedAt())
                .build();
    }
}
