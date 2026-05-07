package com.example.aikb.service.document.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.aikb.dto.document.DocumentProcessRequest;
import com.example.aikb.entity.ChunkEmbedding;
import com.example.aikb.entity.Document;
import com.example.aikb.entity.DocumentChunk;
import com.example.aikb.entity.KnowledgeBase;
import com.example.aikb.entity.TaskRecord;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.ChunkEmbeddingMapper;
import com.example.aikb.mapper.DocumentChunkMapper;
import com.example.aikb.mapper.DocumentMapper;
import com.example.aikb.mapper.KnowledgeBaseMapper;
import com.example.aikb.mapper.TaskRecordMapper;
import com.example.aikb.mq.document.DocumentProcessMessage;
import com.example.aikb.mq.document.DocumentProcessProducer;
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.document.DocumentFileTypeUtils;
import com.example.aikb.service.document.DocumentProcessService;
import com.example.aikb.service.document.SimpleDocumentParser;
import com.example.aikb.service.document.TextSplitter;
import com.example.aikb.service.task.TaskRecordService;
import com.example.aikb.vo.document.DocumentChunkVO;
import com.example.aikb.vo.document.DocumentProcessVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessServiceImpl implements DocumentProcessService {

    private static final String PARSE_STATUS_PROCESSING = "PROCESSING";
    private static final String PARSE_STATUS_SUCCESS = "SUCCESS";
    private static final String PARSE_STATUS_FAILED = "FAILED";
    private static final String LEGACY_PARSE_STATUS_CHUNKING = "CHUNKING";
    private static final String LEGACY_PARSE_STATUS_CHUNKED = "CHUNKED";
    private static final String LEGACY_PARSE_STATUS_DONE = "DONE";
    private static final String EMBEDDING_STATUS_NOT_STARTED = "NOT_STARTED";
    private static final String EMBEDDING_STATUS_PROCESSING = "PROCESSING";
    private static final String TASK_STATUS_SUCCESS = "SUCCESS";
    private static final String TASK_TYPE_DOCUMENT_PROCESS = "DOCUMENT_PROCESS";
    private static final String TASK_TYPE_DOCUMENT_EMBEDDING = "DOCUMENT_EMBEDDING";
    private static final String TASK_STATUS_PROCESSING = "PROCESSING";
    private static final String TASK_STATUS_FAILED = "FAILED";
    private static final String TASK_BIZ_TYPE_DOCUMENT = "DOCUMENT";
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;
    private static final String MESSAGE_DOCUMENT_NOT_FOUND = "Document not found or no permission";
    private static final String MESSAGE_DOCUMENT_PROCESSING = "Document is already processing";
    private static final String MESSAGE_DOCUMENT_ALREADY_PROCESSED = "Document already processed";
    private static final int KNOWLEDGE_BASE_ACTIVE_STATUS = 1;
    private static final int KNOWLEDGE_BASE_NOT_DELETED = 0;

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final ChunkEmbeddingMapper chunkEmbeddingMapper;
    private final TaskRecordMapper taskRecordMapper;
    private final SimpleDocumentParser simpleDocumentParser;
    private final TextSplitter textSplitter;
    private final TaskRecordService taskRecordService;
    private final TransactionTemplate transactionTemplate;
    private final DocumentProcessProducer documentProcessProducer;

    @Override
    public DocumentProcessVO process(Long documentId, DocumentProcessRequest request) {
        DocumentProcessRequest safeRequest = normalizeRequest(request);
        validateChunkOptions(safeRequest);
        if (StringUtils.hasText(safeRequest.getTextContent())) {
            throw new BusinessException("Async document process does not support textContent; upload txt/md file first");
        }

        Long userId = CurrentUser.getUserId();
        Document document = getOwnDocument(documentId, userId);
        DocumentFileTypeUtils.validateProcessSupported(document.getFileType());
        boolean force = Boolean.TRUE.equals(safeRequest.getForce());
        if (!force && isProcessSuccess(document)) {
            int chunkCount = countChunks(document.getId());
            return DocumentProcessVO.builder()
                    .documentId(document.getId())
                    .knowledgeBaseId(document.getKnowledgeBaseId())
                    .chunkCount(chunkCount)
                    .parseStatus(PARSE_STATUS_SUCCESS)
                    .taskStatus(TASK_STATUS_SUCCESS)
                    .build();
        }
        validateProcessAllowed(document, force);

        TaskRecord taskRecord = taskRecordService.createDocumentProcessTask(document, userId);
        taskRecordService.markProcessing(taskRecord.getId());
        String requestId = UUID.randomUUID().toString();

        try {
            markDocumentProcessSubmitted(document.getId());
            documentProcessProducer.send(DocumentProcessMessage.builder()
                    .documentId(document.getId())
                    .knowledgeBaseId(document.getKnowledgeBaseId())
                    .userId(userId)
                    .force(force)
                    .requestId(requestId)
                    .createdAt(LocalDateTime.now())
                    .taskId(taskRecord.getId())
                    .chunkSize(safeRequest.getChunkSize())
                    .overlap(safeRequest.getOverlap())
                    .build());
            log.info("Document process task submitted, documentId={}, taskId={}, requestId={}",
                    document.getId(), taskRecord.getId(), requestId);
            return DocumentProcessVO.builder()
                    .documentId(document.getId())
                    .knowledgeBaseId(document.getKnowledgeBaseId())
                    .chunkCount(document.getChunkCount())
                    .parseStatus(PARSE_STATUS_PROCESSING)
                    .taskId(taskRecord.getId())
                    .taskStatus(TASK_STATUS_PROCESSING)
                    .build();
        } catch (AmqpException ex) {
            String safeError = lifecycleError("RabbitMQ message send failed: " + ex.getMessage());
            taskRecordService.markFailed(taskRecord.getId(), safeError);
            markDocumentProcessFailed(document.getId(), safeError);
            log.warn("Submit document process message failed, documentId={}, taskId={}, requestId={}",
                    document.getId(), taskRecord.getId(), requestId, ex);
            throw new BusinessException(50300, "RabbitMQ unavailable, document process task was not submitted", 503);
        } catch (RuntimeException ex) {
            String safeError = lifecycleError(ex.getMessage());
            taskRecordService.markFailed(taskRecord.getId(), safeError);
            markDocumentProcessFailed(document.getId(), safeError);
            throw ex;
        }
    }

    @Override
    public DocumentProcessVO processFromMessage(DocumentProcessMessage message) {
        DocumentProcessRequest safeRequest = normalizeRequest(fromMessage(message));
        validateChunkOptions(safeRequest);

        try {
            DocumentProcessVO result = transactionTemplate.execute(status ->
                    processMessageInTransaction(message, safeRequest));
            if (result == null) {
                throw new BusinessException("Document process result is empty");
            }
            if (message.getTaskId() != null) {
                taskRecordService.markSuccess(message.getTaskId());
            }
            result.setTaskStatus(TASK_STATUS_SUCCESS);
            return result;
        } catch (RuntimeException ex) {
            String safeError = lifecycleError(ex.getMessage());
            if (message.getTaskId() != null) {
                taskRecordService.markFailed(message.getTaskId(), safeError);
            }
            if (!MESSAGE_DOCUMENT_PROCESSING.equals(safeError) && message.getDocumentId() != null) {
                markDocumentProcessFailed(message.getDocumentId(), safeError);
            }
            throw ex;
        }
    }

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

    private DocumentProcessVO processMessageInTransaction(
            DocumentProcessMessage message, DocumentProcessRequest safeRequest) {
        Document latestDocument = documentMapper.selectById(message.getDocumentId());
        if (latestDocument == null) {
            throw new BusinessException(40400, MESSAGE_DOCUMENT_NOT_FOUND);
        }
        ensureDocumentStillValid(latestDocument);
        DocumentFileTypeUtils.validateProcessSupported(latestDocument.getFileType());
        TaskRecord messageTask = getExecutableMessageTask(message, latestDocument.getId());
        if (TASK_STATUS_SUCCESS.equals(messageTask.getStatus())) {
            return DocumentProcessVO.builder()
                    .documentId(latestDocument.getId())
                    .knowledgeBaseId(latestDocument.getKnowledgeBaseId())
                    .chunkCount(countChunks(latestDocument.getId()))
                    .parseStatus(PARSE_STATUS_SUCCESS)
                    .taskId(message.getTaskId())
                    .build();
        }

        boolean force = Boolean.TRUE.equals(safeRequest.getForce());
        if (!force && isProcessSuccess(latestDocument)) {
            return DocumentProcessVO.builder()
                    .documentId(latestDocument.getId())
                    .knowledgeBaseId(latestDocument.getKnowledgeBaseId())
                    .chunkCount(countChunks(latestDocument.getId()))
                    .parseStatus(PARSE_STATUS_SUCCESS)
                    .taskId(message.getTaskId())
                    .build();
        }
        validateConsumerProcessState(latestDocument, force, message.getTaskId());

        String text = simpleDocumentParser.parse(latestDocument, safeRequest.getTextContent());
        List<String> chunks = textSplitter.split(text, safeRequest.getChunkSize(), safeRequest.getOverlap());
        if (chunks.isEmpty()) {
            throw new BusinessException("Document content is empty, no chunks generated");
        }

        rebuildChunks(latestDocument, chunks);
        updateDocumentProcessSuccess(latestDocument.getId(), chunks.size());

        return DocumentProcessVO.builder()
                .documentId(latestDocument.getId())
                .knowledgeBaseId(latestDocument.getKnowledgeBaseId())
                .chunkCount(chunks.size())
                .parseStatus(PARSE_STATUS_SUCCESS)
                .taskId(message.getTaskId())
                .build();
    }

    private DocumentProcessRequest fromMessage(DocumentProcessMessage message) {
        DocumentProcessRequest request = new DocumentProcessRequest();
        request.setChunkSize(message.getChunkSize());
        request.setOverlap(message.getOverlap());
        request.setForce(message.getForce());
        return request;
    }

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

    private void validateChunkOptions(DocumentProcessRequest request) {
        if (request.getOverlap() >= request.getChunkSize()) {
            throw new BusinessException("overlap must be less than chunkSize");
        }
    }

    private void rebuildChunks(Document document, List<String> chunks) {
        chunkEmbeddingMapper.delete(new LambdaQueryWrapper<ChunkEmbedding>()
                .eq(ChunkEmbedding::getDocumentId, document.getId()));
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

    private Document getOwnDocument(Long documentId, Long userId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(40400, MESSAGE_DOCUMENT_NOT_FOUND);
        }

        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, document.getKnowledgeBaseId())
                .eq(KnowledgeBase::getOwnerId, userId)
                .eq(KnowledgeBase::getStatus, KNOWLEDGE_BASE_ACTIVE_STATUS)
                .eq(KnowledgeBase::getDeleted, KNOWLEDGE_BASE_NOT_DELETED)
                .last("LIMIT 1"));
        if (knowledgeBase == null) {
            throw new BusinessException(40400, MESSAGE_DOCUMENT_NOT_FOUND);
        }
        return document;
    }

    private void ensureDocumentStillValid(Document document) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, document.getKnowledgeBaseId())
                .eq(KnowledgeBase::getStatus, KNOWLEDGE_BASE_ACTIVE_STATUS)
                .eq(KnowledgeBase::getDeleted, KNOWLEDGE_BASE_NOT_DELETED)
                .last("LIMIT 1"));
        if (knowledgeBase == null) {
            throw new BusinessException(40400, MESSAGE_DOCUMENT_NOT_FOUND);
        }
    }

    private TaskRecord getExecutableMessageTask(DocumentProcessMessage message, Long documentId) {
        if (message.getTaskId() == null) {
            throw new BusinessException("Document process message taskId is required");
        }
        TaskRecord taskRecord = taskRecordMapper.selectById(message.getTaskId());
        if (taskRecord == null
                || !TASK_BIZ_TYPE_DOCUMENT.equals(taskRecord.getBizType())
                || !documentId.equals(taskRecord.getBizId())
                || !TASK_TYPE_DOCUMENT_PROCESS.equals(taskRecord.getTaskType())) {
            throw new BusinessException("Document process message task does not match document");
        }
        if (TASK_STATUS_SUCCESS.equals(taskRecord.getStatus())) {
            return taskRecord;
        }
        if (!TASK_STATUS_PROCESSING.equals(taskRecord.getStatus())) {
            throw new BusinessException(MESSAGE_DOCUMENT_PROCESSING);
        }
        return taskRecord;
    }

    private void validateProcessAllowed(Document document, boolean force) {
        validateLatestProcessState(document, force);
        if (hasRunningDocumentTask(document.getId())) {
            throw new BusinessException(MESSAGE_DOCUMENT_PROCESSING);
        }
    }

    private void validateLatestProcessState(Document document, boolean force) {
        String parseStatus = document.getParseStatus();
        if (PARSE_STATUS_PROCESSING.equals(parseStatus) || LEGACY_PARSE_STATUS_CHUNKING.equals(parseStatus)) {
            throw new BusinessException(MESSAGE_DOCUMENT_PROCESSING);
        }
        if (!force && isProcessSuccess(document)) {
            throw new BusinessException(MESSAGE_DOCUMENT_ALREADY_PROCESSED);
        }
    }

    private void validateConsumerProcessState(Document document, boolean force, Long taskId) {
        if (!force && isProcessSuccess(document)) {
            return;
        }

        String parseStatus = document.getParseStatus();
        boolean currentTaskMatches = taskId != null
                && TASK_TYPE_DOCUMENT_PROCESS.equals(document.getLatestTaskType())
                && TASK_STATUS_PROCESSING.equals(document.getLatestTaskStatus());
        if ((PARSE_STATUS_PROCESSING.equals(parseStatus) || LEGACY_PARSE_STATUS_CHUNKING.equals(parseStatus))
                && !currentTaskMatches) {
            throw new BusinessException(MESSAGE_DOCUMENT_PROCESSING);
        }
    }

    private boolean isProcessSuccess(Document document) {
        String parseStatus = document.getParseStatus();
        return PARSE_STATUS_SUCCESS.equals(parseStatus)
                || LEGACY_PARSE_STATUS_CHUNKED.equals(parseStatus)
                || LEGACY_PARSE_STATUS_DONE.equals(parseStatus);
    }

    private boolean hasRunningDocumentTask(Long documentId) {
        Long count = taskRecordMapper.selectCount(new LambdaQueryWrapper<TaskRecord>()
                .eq(TaskRecord::getBizType, TASK_BIZ_TYPE_DOCUMENT)
                .eq(TaskRecord::getBizId, documentId)
                .eq(TaskRecord::getStatus, TASK_STATUS_PROCESSING)
                .in(TaskRecord::getTaskType, TASK_TYPE_DOCUMENT_PROCESS, TASK_TYPE_DOCUMENT_EMBEDDING));
        return count != null && count > 0;
    }

    private void markDocumentProcessSubmitted(Long documentId) {
        int rows = documentMapper.update(null, new LambdaUpdateWrapper<Document>()
                .eq(Document::getId, documentId)
                .ne(Document::getParseStatus, PARSE_STATUS_PROCESSING)
                .ne(Document::getEmbeddingStatus, EMBEDDING_STATUS_PROCESSING)
                .set(Document::getParseStatus, PARSE_STATUS_PROCESSING)
                .set(Document::getLatestTaskType, TASK_TYPE_DOCUMENT_PROCESS)
                .set(Document::getLatestTaskStatus, TASK_STATUS_PROCESSING)
                .set(Document::getLatestErrorMessage, null));
        if (rows != 1) {
            throw new BusinessException(MESSAGE_DOCUMENT_PROCESSING);
        }
    }

    private void updateDocumentProcessSuccess(Long documentId, int chunkCount) {
        Document update = new Document();
        update.setId(documentId);
        update.setParseStatus(PARSE_STATUS_SUCCESS);
        update.setChunkCount(chunkCount);
        update.setEmbeddingStatus(EMBEDDING_STATUS_NOT_STARTED);
        update.setEmbeddedChunkCount(0);
        update.setLatestTaskType(TASK_TYPE_DOCUMENT_PROCESS);
        update.setLatestTaskStatus(TASK_STATUS_SUCCESS);
        update.setLatestErrorMessage(null);
        documentMapper.updateById(update);
    }

    private void markDocumentProcessFailed(Long documentId, String errorMessage) {
        Document update = new Document();
        update.setId(documentId);
        update.setParseStatus(PARSE_STATUS_FAILED);
        update.setLatestTaskType(TASK_TYPE_DOCUMENT_PROCESS);
        update.setLatestTaskStatus(TASK_STATUS_FAILED);
        update.setLatestErrorMessage(errorMessage);
        documentMapper.updateById(update);
    }

    private int countChunks(Long documentId) {
        return Math.toIntExact(documentChunkMapper.selectCount(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, documentId)));
    }

    private String lifecycleError(String errorMessage) {
        String message = StringUtils.hasText(errorMessage) ? errorMessage.trim() : "Document process failed";
        message = message.replaceAll("(?i)api[_-]?key\\s*[:=]\\s*\\S+", "apiKey=***");
        if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

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
