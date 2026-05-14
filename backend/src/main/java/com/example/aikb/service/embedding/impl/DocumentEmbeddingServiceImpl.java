package com.example.aikb.service.embedding.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.aikb.common.LogSanitizer;
import com.example.aikb.config.AppEmbeddingProperties;
import com.example.aikb.dto.document.DocumentEmbeddingRequest;
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
import com.example.aikb.mq.document.DocumentEmbeddingMessage;
import com.example.aikb.mq.document.DocumentEmbeddingProducer;
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.document.DocumentFileTypeUtils;
import com.example.aikb.service.embedding.DocumentEmbeddingService;
import com.example.aikb.service.embedding.EmbeddingClient;
import com.example.aikb.service.embedding.EmbeddingResult;
import com.example.aikb.service.task.TaskRecordService;
import com.example.aikb.vo.document.ChunkEmbeddingStatusVO;
import com.example.aikb.vo.document.DocumentEmbeddingProgressVO;
import com.example.aikb.vo.document.DocumentEmbeddingStatusVO;
import com.example.aikb.vo.document.DocumentEmbeddingVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentEmbeddingServiceImpl implements DocumentEmbeddingService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;
    private static final String DOCUMENT_STATUS_NOT_CHUNKED = "NOT_CHUNKED";
    private static final String DOCUMENT_STATUS_PENDING = "PENDING";
    private static final String DOCUMENT_STATUS_PROCESSING = "PROCESSING";
    private static final String DOCUMENT_STATUS_SUCCESS = "SUCCESS";
    private static final String DOCUMENT_STATUS_FAILED = "FAILED";
    private static final String DOCUMENT_STATUS_PARTIAL = "PARTIAL_SUCCESS";
    private static final String PARSE_STATUS_SUCCESS = "SUCCESS";
    private static final String LEGACY_PARSE_STATUS_CHUNKED = "CHUNKED";
    private static final String LEGACY_PARSE_STATUS_DONE = "DONE";
    private static final String TASK_BIZ_TYPE_DOCUMENT = "DOCUMENT";
    private static final String TASK_TYPE_DOCUMENT_PROCESS = "DOCUMENT_PROCESS";
    private static final String TASK_TYPE_DOCUMENT_EMBEDDING = "DOCUMENT_EMBEDDING";
    private static final String MESSAGE_DOCUMENT_PROCESSING = "Document is already processing";
    private static final int KNOWLEDGE_BASE_ACTIVE_STATUS = 1;
    private static final int KNOWLEDGE_BASE_NOT_DELETED = 0;

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final ChunkEmbeddingMapper chunkEmbeddingMapper;
    private final TaskRecordMapper taskRecordMapper;
    private final EmbeddingClient embeddingClient;
    private final TaskRecordService taskRecordService;
    private final ObjectMapper objectMapper;
    private final AppEmbeddingProperties embeddingProperties;
    private final DocumentEmbeddingProducer documentEmbeddingProducer;
    private final TransactionTemplate transactionTemplate;

    @Override
    public DocumentEmbeddingVO embedDocument(Long documentId, DocumentEmbeddingRequest request) {
        DocumentEmbeddingRequest safeRequest = request == null ? new DocumentEmbeddingRequest() : request;
        Long userId = CurrentUser.getUserId();
        Document document = getOwnDocument(documentId, userId);
        DocumentFileTypeUtils.validateProcessSupported(document.getFileType());
        validateEmbeddingAllowed(document);

        boolean force = Boolean.TRUE.equals(safeRequest.getForce());
        List<DocumentChunk> chunks = listEmbeddableDocumentChunks(document.getId());
        if (chunks.isEmpty()) {
            throw new BusinessException("Document has no chunks, process it first");
        }
        String embeddingModel = resolveEmbeddingModel(safeRequest);
        if (!force && isEmbeddingSuccess(document, chunks.size())) {
            return DocumentEmbeddingVO.builder()
                    .documentId(document.getId())
                    .knowledgeBaseId(document.getKnowledgeBaseId())
                    .total(0)
                    .successCount(0)
                    .failedCount(0)
                    .embeddingModel(embeddingModel)
                    .taskStatus(STATUS_SUCCESS)
                    .build();
        }

        TaskRecord taskRecord = taskRecordService.createDocumentEmbeddingTask(document, userId);
        String requestId = UUID.randomUUID().toString();

        try {
            documentEmbeddingProducer.send(DocumentEmbeddingMessage.builder()
                    .documentId(document.getId())
                    .knowledgeBaseId(document.getKnowledgeBaseId())
                    .userId(userId)
                    .force(force)
                    .requestId(requestId)
                    .createdAt(LocalDateTime.now())
                    .taskId(taskRecord.getId())
                    .embeddingModel(embeddingModel)
                    .build());
            log.info("Document embedding task submitted, documentId={}, taskId={}, requestId={}",
                    document.getId(), taskRecord.getId(), requestId);
            return DocumentEmbeddingVO.builder()
                    .documentId(document.getId())
                    .knowledgeBaseId(document.getKnowledgeBaseId())
                    .total(chunks.size())
                    .successCount(0)
                    .failedCount(0)
                    .embeddingModel(embeddingModel)
                    .taskId(taskRecord.getId())
                    .taskStatus(STATUS_PENDING)
                    .build();
        } catch (AmqpException ex) {
            String safeError = truncateError("RabbitMQ message send failed: " + ex.getMessage());
            taskRecordService.markFailed(taskRecord.getId(), safeError);
            updateDocumentEmbeddingFailed(document.getId(), safeError);
            log.warn("Submit document embedding message failed, documentId={}, taskId={}, requestId={}",
                    document.getId(), taskRecord.getId(), requestId, ex);
            throw new BusinessException(50300, "RabbitMQ unavailable, document embedding task was not submitted", 503);
        } catch (RuntimeException ex) {
            String safeError = truncateError(ex.getMessage());
            taskRecordService.markFailed(taskRecord.getId(), safeError);
            updateDocumentEmbeddingFailed(document.getId(), safeError);
            throw ex;
        }
    }

    @Override
    public DocumentEmbeddingVO embedDocumentFromMessage(DocumentEmbeddingMessage message) {
        try {
            log.info("Start async document embedding, documentId={}, knowledgeBaseId={}, taskId={}, requestId={}",
                    message.getDocumentId(), message.getKnowledgeBaseId(), message.getTaskId(), message.getRequestId());
            DocumentEmbeddingVO result = doEmbedDocumentFromMessage(message);
            if (message.getTaskId() != null) {
                taskRecordService.markSuccess(message.getTaskId());
            }
            log.info("Async document embedding finished, documentId={}, knowledgeBaseId={}, taskId={}, total={}, successCount={}, failedCount={}",
                    message.getDocumentId(),
                    message.getKnowledgeBaseId(),
                    message.getTaskId(),
                    result.getTotal(),
                    result.getSuccessCount(),
                    result.getFailedCount());
            result.setTaskStatus(STATUS_SUCCESS);
            return result;
        } catch (RuntimeException ex) {
            String safeError = truncateError(ex.getMessage());
            Throwable rootCause = rootCause(ex);
            log.error("Async document embedding failed, documentId={}, knowledgeBaseId={}, taskId={}, errorType={}, error={}, rootCauseType={}, rootCause={}",
                    message == null ? null : message.getDocumentId(),
                    message == null ? null : message.getKnowledgeBaseId(),
                    message == null ? null : message.getTaskId(),
                    ex.getClass().getName(),
                    LogSanitizer.safeMessage(ex.getMessage()),
                    rootCause == null ? null : rootCause.getClass().getName(),
                    safeRootCauseMessage(rootCause),
                    ex);
            if (message != null && message.getTaskId() != null) {
                taskRecordService.markFailed(message.getTaskId(), safeError);
            }
            if (message != null
                    && message.getDocumentId() != null
                    && isLatestEmbeddingTask(message.getDocumentId(), message.getTaskId())) {
                updateDocumentEmbeddingFailed(message.getDocumentId(), safeError);
            }
            throw ex;
        }
    }

    @Override
    public DocumentEmbeddingStatusVO getEmbeddingStatus(Long documentId) {
        Long userId = CurrentUser.getUserId();
        Document document = getOwnDocument(documentId, userId);
        List<DocumentChunk> chunks = listDocumentChunks(document.getId());
        if (chunks.isEmpty()) {
            return DocumentEmbeddingStatusVO.builder()
                    .documentId(document.getId())
                    .knowledgeBaseId(document.getKnowledgeBaseId())
                    .totalChunks(0)
                    .successCount(0)
                    .failedCount(0)
                    .pendingCount(0)
                    .status(DOCUMENT_STATUS_NOT_CHUNKED)
                    .chunks(List.of())
                    .build();
        }

        Map<Long, ChunkEmbedding> embeddingMap = chunkEmbeddingMapper.selectList(new LambdaQueryWrapper<ChunkEmbedding>()
                        .eq(ChunkEmbedding::getDocumentId, document.getId()))
                .stream()
                .collect(Collectors.toMap(ChunkEmbedding::getChunkId, Function.identity(), (left, right) -> left));

        List<ChunkEmbeddingStatusVO> chunkStatuses = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        int pendingCount = 0;
        int processingCount = 0;
        for (DocumentChunk chunk : chunks) {
            ChunkEmbedding embedding = embeddingMap.get(chunk.getId());
            String status = embedding == null ? STATUS_PENDING : embedding.getStatus();
            if (STATUS_SUCCESS.equals(status)) {
                successCount++;
            } else if (STATUS_FAILED.equals(status)) {
                failedCount++;
            } else if (STATUS_PROCESSING.equals(status)) {
                processingCount++;
            } else {
                pendingCount++;
            }

            chunkStatuses.add(toStatusVO(chunk, embedding, status));
        }

        return DocumentEmbeddingStatusVO.builder()
                .documentId(document.getId())
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .totalChunks(chunks.size())
                .successCount(successCount)
                .failedCount(failedCount)
                .pendingCount(pendingCount)
                .status(resolveDocumentEmbeddingStatus(
                        chunks.size(), successCount, failedCount, pendingCount, processingCount))
                .chunks(chunkStatuses)
                .build();
    }

    @Override
    public DocumentEmbeddingProgressVO getEmbeddingProgress(Long documentId) {
        Long userId = CurrentUser.getUserId();
        Document document = getOwnDocument(documentId, userId);
        int totalChunks = document.getChunkCount() != null ? document.getChunkCount() : 0;
        int embeddedChunks = document.getEmbeddedChunkCount() != null ? document.getEmbeddedChunkCount() : 0;
        int progress = 0;
        if (totalChunks > 0) {
            progress = (int) ((double) embeddedChunks / totalChunks * 100);
            if (progress > 100) {
                progress = 100;
            }
        }
        return DocumentEmbeddingProgressVO.builder()
                .documentId(document.getId())
                .status(document.getEmbeddingStatus())
                .totalChunks(totalChunks)
                .embeddedChunks(embeddedChunks)
                .progress(progress)
                .errorMessage(document.getLatestErrorMessage())
                .build();
    }

    private DocumentEmbeddingVO doEmbedDocumentFromMessage(DocumentEmbeddingMessage message) {
        Document document = documentMapper.selectById(message.getDocumentId());
        if (document == null) {
            throw new BusinessException(40400, "Document not found");
        }
        ensureDocumentStillValid(document);
        DocumentFileTypeUtils.validateProcessSupported(document.getFileType());
        TaskRecord messageTask = getExecutableMessageTask(message, document);
        if (STATUS_SUCCESS.equals(messageTask.getStatus())) {
            return buildNoopResult(document, message.getTaskId(), resolveEmbeddingModel(message.getEmbeddingModel()));
        }
        if (STATUS_FAILED.equals(messageTask.getStatus())) {
            throw new BusinessException("Document embedding task already failed");
        }
        if (STATUS_PENDING.equals(messageTask.getStatus())) {
            taskRecordService.markProcessing(messageTask.getId());
        }
        markDocumentEmbeddingSubmitted(document.getId());
        if (!isParseSuccess(document)) {
            throw new BusinessException("Document has not been processed successfully");
        }

        List<DocumentChunk> allChunks = listEmbeddableDocumentChunks(document.getId());
        if (allChunks.isEmpty()) {
            throw new BusinessException("Document has no chunks, process it first");
        }

        boolean force = Boolean.TRUE.equals(message.getForce());
        String embeddingModel = resolveEmbeddingModel(message.getEmbeddingModel());
        if (!force && isEmbeddingSuccess(document, allChunks.size())) {
            return buildNoopResult(document, message.getTaskId(), embeddingModel);
        }
        if (force) {
            transactionTemplate.executeWithoutResult(status -> clearDocumentEmbeddingsForRebuild(document.getId()));
        }

        List<DocumentChunk> chunks = force ? allChunks : filterChunksWithoutSuccessfulEmbedding(allChunks);
        if (chunks.isEmpty()) {
            transactionTemplate.executeWithoutResult(status ->
                    updateDocumentEmbeddingFinished(document.getId(), allChunks.size()));
            return buildNoopResult(document, message.getTaskId(), embeddingModel);
        }
        int batchSize = embeddingProperties.getBatchSize();
        int total = chunks.size();
        int successCount = 0;
        int failCount = 0;

        log.info("Async document embedding chunks ready, documentId={}, knowledgeBaseId={}, taskId={}, totalChunkCount={}, embeddableChunkCount={}, model={}, batchSize={}",
                document.getId(),
                document.getKnowledgeBaseId(),
                message.getTaskId(),
                allChunks.size(),
                total,
                embeddingModel,
                batchSize);

        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            List<DocumentChunk> batch = chunks.subList(i, end);
            List<EmbeddedChunkResult> batchResults = new ArrayList<>(batchSize);

            for (DocumentChunk chunk : batch) {
                try {
                    long chunkStart = System.currentTimeMillis();
                    int contentLength = chunk.getContent() == null ? 0 : chunk.getContent().length();
                    log.debug("Embedding chunk, documentId={}, chunkId={}, contentLength={}",
                            document.getId(), chunk.getId(), contentLength);
                    EmbeddingResult result = embeddingClient.embed(chunk.getId(), chunk.getContent(), embeddingModel);
                    if (result.getVector() == null || result.getVector().isEmpty()) {
                        throw new BusinessException("embedding vector is empty");
                    }
                    log.debug("Embedding chunk finished, documentId={}, chunkId={}, durationMs={}",
                            document.getId(), chunk.getId(), System.currentTimeMillis() - chunkStart);
                    batchResults.add(new EmbeddedChunkResult(chunk, result));
                    successCount++;
                } catch (Exception ex) {
                    failCount++;
                    log.warn("Embedding chunk failed, documentId={}, chunkId={}, error={}",
                            document.getId(), chunk.getId(), LogSanitizer.safeMessage(ex.getMessage()));
                    ChunkEmbedding failedRecord = new ChunkEmbedding();
                    failedRecord.setDocumentId(document.getId());
                    failedRecord.setKnowledgeBaseId(document.getKnowledgeBaseId());
                    failedRecord.setChunkId(chunk.getId());
                    failedRecord.setEmbeddingModel(embeddingModel);
                    failedRecord.setStatus(STATUS_FAILED);
                    failedRecord.setEmbeddingError(truncateError(ex.getMessage()));
                    failedRecord.setEmbeddedAt(LocalDateTime.now());
                    chunkEmbeddingMapper.insert(failedRecord);
                }
            }

            if (!batchResults.isEmpty()) {
                transactionTemplate.executeWithoutResult(status ->
                        persistEmbeddingResults(document, batchResults, force, embeddingModel));
            }

            updateDocumentEmbeddingInProgress(document.getId());
        }

        updateDocumentEmbeddingFinished(document.getId(), allChunks.size());

        return DocumentEmbeddingVO.builder()
                .documentId(document.getId())
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .total(total)
                .successCount(successCount)
                .failedCount(failCount)
                .embeddingModel(embeddingModel)
                .taskId(message.getTaskId())
                .taskStatus(failCount > 0 && failCount == total ? STATUS_FAILED : STATUS_SUCCESS)
                .build();
    }

    private void persistEmbeddingResults(
            Document document, List<EmbeddedChunkResult> results, boolean force, String embeddingModel) {
        if (!force) {
            List<Long> chunkIds = results.stream().map(result -> result.chunk().getId()).toList();
            if (!chunkIds.isEmpty()) {
                chunkEmbeddingMapper.delete(new LambdaQueryWrapper<ChunkEmbedding>()
                        .in(ChunkEmbedding::getChunkId, chunkIds));
            }
        }

        for (EmbeddedChunkResult result : results) {
            ChunkEmbedding embedding = new ChunkEmbedding();
            embedding.setDocumentId(document.getId());
            embedding.setKnowledgeBaseId(document.getKnowledgeBaseId());
            embedding.setChunkId(result.chunk().getId());
            embedding.setEmbeddingModel(result.result().getEmbeddingModel());
            embedding.setVectorId(result.result().getVectorId());
            embedding.setVectorJson(serializeVector(result.result().getVector()));
            embedding.setStatus(STATUS_SUCCESS);
            embedding.setEmbeddingError(null);
            embedding.setEmbeddedAt(LocalDateTime.now());
            chunkEmbeddingMapper.insert(embedding);
        }
    }

    private void clearDocumentEmbeddingsForRebuild(Long documentId) {
        chunkEmbeddingMapper.delete(new LambdaQueryWrapper<ChunkEmbedding>()
                .eq(ChunkEmbedding::getDocumentId, documentId));
        Document update = new Document();
        update.setId(documentId);
        update.setEmbeddedChunkCount(0);
        documentMapper.updateById(update);
    }

    private DocumentEmbeddingVO buildNoopResult(Document document, Long taskId, String embeddingModel) {
        return DocumentEmbeddingVO.builder()
                .documentId(document.getId())
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .total(0)
                .successCount(0)
                .failedCount(0)
                .embeddingModel(embeddingModel)
                .taskId(taskId)
                .taskStatus(STATUS_SUCCESS)
                .build();
    }

    private List<DocumentChunk> listDocumentChunks(Long documentId) {
        return documentChunkMapper.selectList(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, documentId)
                .orderByAsc(DocumentChunk::getChunkIndex));
    }

    private List<DocumentChunk> listEmbeddableDocumentChunks(Long documentId) {
        return listDocumentChunks(documentId)
                .stream()
                .filter(chunk -> StringUtils.hasText(chunk.getContent()))
                .toList();
    }

    private void validateEmbeddingAllowed(Document document) {
        if (!isParseSuccess(document)) {
            throw new BusinessException("Document has not been processed successfully");
        }
        if (DOCUMENT_STATUS_PROCESSING.equals(document.getEmbeddingStatus())) {
            throw new BusinessException(MESSAGE_DOCUMENT_PROCESSING);
        }
        Long count = taskRecordMapper.selectCount(new LambdaQueryWrapper<TaskRecord>()
                .eq(TaskRecord::getBizType, TASK_BIZ_TYPE_DOCUMENT)
                .eq(TaskRecord::getBizId, document.getId())
                .in(TaskRecord::getStatus, STATUS_PENDING, STATUS_PROCESSING)
                .in(TaskRecord::getTaskType, TASK_TYPE_DOCUMENT_PROCESS, TASK_TYPE_DOCUMENT_EMBEDDING));
        if (count != null && count > 0) {
            throw new BusinessException(MESSAGE_DOCUMENT_PROCESSING);
        }
    }

    private boolean isParseSuccess(Document document) {
        String parseStatus = document.getParseStatus();
        return PARSE_STATUS_SUCCESS.equals(parseStatus)
                || LEGACY_PARSE_STATUS_CHUNKED.equals(parseStatus)
                || LEGACY_PARSE_STATUS_DONE.equals(parseStatus);
    }

    private boolean isEmbeddingSuccess(Document document, int chunkCount) {
        return DOCUMENT_STATUS_SUCCESS.equals(document.getEmbeddingStatus())
                && countSuccessfulEmbeddings(document.getId()) >= chunkCount;
    }

    private List<DocumentChunk> filterChunksWithoutSuccessfulEmbedding(List<DocumentChunk> chunks) {
        List<Long> chunkIds = chunks.stream().map(DocumentChunk::getId).toList();
        if (chunkIds.isEmpty()) {
            return List.of();
        }
        List<Long> successChunkIds = chunkEmbeddingMapper.selectList(new LambdaQueryWrapper<ChunkEmbedding>()
                        .in(ChunkEmbedding::getChunkId, chunkIds)
                        .eq(ChunkEmbedding::getStatus, STATUS_SUCCESS))
                .stream()
                .map(ChunkEmbedding::getChunkId)
                .toList();
        return chunks.stream()
                .filter(chunk -> !successChunkIds.contains(chunk.getId()))
                .toList();
    }

    private int countSuccessfulEmbeddings(Long documentId) {
        return Math.toIntExact(chunkEmbeddingMapper.selectCount(new LambdaQueryWrapper<ChunkEmbedding>()
                .eq(ChunkEmbedding::getDocumentId, documentId)
                .eq(ChunkEmbedding::getStatus, STATUS_SUCCESS)));
    }

    private int countFailedEmbeddings(Long documentId) {
        return Math.toIntExact(chunkEmbeddingMapper.selectCount(new LambdaQueryWrapper<ChunkEmbedding>()
                .eq(ChunkEmbedding::getDocumentId, documentId)
                .eq(ChunkEmbedding::getStatus, STATUS_FAILED)));
    }

    private void markDocumentEmbeddingSubmitted(Long documentId) {
        int rows = documentMapper.update(null, new LambdaUpdateWrapper<Document>()
                .eq(Document::getId, documentId)
                .ne(Document::getParseStatus, DOCUMENT_STATUS_PROCESSING)
                .ne(Document::getEmbeddingStatus, DOCUMENT_STATUS_PROCESSING)
                .set(Document::getEmbeddingStatus, DOCUMENT_STATUS_PROCESSING)
                .set(Document::getLatestTaskType, TASK_TYPE_DOCUMENT_EMBEDDING)
                .set(Document::getLatestTaskStatus, STATUS_PROCESSING)
                .set(Document::getLatestErrorMessage, null));
        if (rows != 1) {
            throw new BusinessException(MESSAGE_DOCUMENT_PROCESSING);
        }
    }

    private void updateDocumentEmbeddingFinished(Long documentId, int totalChunkCount) {
        int embeddedCount = countSuccessfulEmbeddings(documentId);
        int failedCount = countFailedEmbeddings(documentId);
        String status = resolveDocumentEmbeddingStatus(
                totalChunkCount, embeddedCount, failedCount, 0, 0);
        Document update = new Document();
        update.setId(documentId);
        update.setEmbeddingStatus(status);
        update.setChunkCount(totalChunkCount);
        update.setEmbeddedChunkCount(embeddedCount);
        update.setLatestTaskType(TASK_TYPE_DOCUMENT_EMBEDDING);
        update.setLatestTaskStatus(failedCount > 0 ? STATUS_FAILED : STATUS_SUCCESS);
        update.setLatestErrorMessage(failedCount > 0 ? "Document embedding failed chunks: " + failedCount : null);
        documentMapper.updateById(update);
    }

    private void updateDocumentEmbeddingInProgress(Long documentId) {
        int totalSuccess = countSuccessfulEmbeddings(documentId);
        int totalFailed = countFailedEmbeddings(documentId);
        Document update = new Document();
        update.setId(documentId);
        update.setEmbeddedChunkCount(totalSuccess);
        update.setLatestTaskType(TASK_TYPE_DOCUMENT_EMBEDDING);
        update.setLatestTaskStatus(STATUS_PROCESSING);
        if (totalFailed > 0) {
            update.setLatestErrorMessage("Document embedding failed chunks: " + totalFailed);
        }
        documentMapper.updateById(update);
    }

    private void updateDocumentEmbeddingFailed(Long documentId, String errorMessage) {
        Document update = new Document();
        update.setId(documentId);
        update.setEmbeddingStatus(DOCUMENT_STATUS_FAILED);
        update.setEmbeddedChunkCount(countSuccessfulEmbeddings(documentId));
        update.setLatestTaskType(TASK_TYPE_DOCUMENT_EMBEDDING);
        update.setLatestTaskStatus(STATUS_FAILED);
        update.setLatestErrorMessage(truncateError(errorMessage));
        documentMapper.updateById(update);
    }

    private Document getOwnDocument(Long documentId, Long userId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(40400, "Document not found");
        }

        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, document.getKnowledgeBaseId())
                .eq(KnowledgeBase::getOwnerId, userId)
                .eq(KnowledgeBase::getStatus, KNOWLEDGE_BASE_ACTIVE_STATUS)
                .eq(KnowledgeBase::getDeleted, KNOWLEDGE_BASE_NOT_DELETED)
                .last("LIMIT 1"));
        if (knowledgeBase == null) {
            throw new BusinessException(40400, "Document not found");
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
            throw new BusinessException(40400, "Document not found");
        }
    }

    private TaskRecord getExecutableMessageTask(DocumentEmbeddingMessage message, Document document) {
        if (message.getTaskId() == null) {
            throw new BusinessException("Document embedding message taskId is required");
        }
        TaskRecord taskRecord = taskRecordMapper.selectById(message.getTaskId());
        if (taskRecord == null
                || !TASK_BIZ_TYPE_DOCUMENT.equals(taskRecord.getBizType())
                || !document.getId().equals(taskRecord.getBizId())
                || !TASK_TYPE_DOCUMENT_EMBEDDING.equals(taskRecord.getTaskType())) {
            throw new BusinessException("Document embedding message task does not match document");
        }
        if (STATUS_SUCCESS.equals(taskRecord.getStatus())) {
            return taskRecord;
        }
        TaskRecord latestEmbeddingTask = getLatestEmbeddingTask(document.getId());
        if (latestEmbeddingTask == null || !message.getTaskId().equals(latestEmbeddingTask.getId())) {
            throw new BusinessException("Document embedding message is not the latest task");
        }
        if (!STATUS_PENDING.equals(taskRecord.getStatus()) && !STATUS_PROCESSING.equals(taskRecord.getStatus())) {
            throw new BusinessException(MESSAGE_DOCUMENT_PROCESSING);
        }
        return taskRecord;
    }

    private TaskRecord getLatestEmbeddingTask(Long documentId) {
        return taskRecordMapper.selectOne(new LambdaQueryWrapper<TaskRecord>()
                .eq(TaskRecord::getBizType, TASK_BIZ_TYPE_DOCUMENT)
                .eq(TaskRecord::getBizId, documentId)
                .eq(TaskRecord::getTaskType, TASK_TYPE_DOCUMENT_EMBEDDING)
                .orderByDesc(TaskRecord::getCreatedAt)
                .orderByDesc(TaskRecord::getId)
                .last("LIMIT 1"));
    }

    private boolean isLatestEmbeddingTask(Long documentId, Long taskId) {
        if (documentId == null || taskId == null) {
            return false;
        }
        TaskRecord latestTask = getLatestEmbeddingTask(documentId);
        return latestTask != null && taskId.equals(latestTask.getId());
    }

    private String resolveEmbeddingModel(DocumentEmbeddingRequest request) {
        if (request != null && StringUtils.hasText(request.getEmbeddingModel())) {
            return request.getEmbeddingModel().trim();
        }
        return embeddingProperties.getModel();
    }

    private String resolveEmbeddingModel(String embeddingModel) {
        if (StringUtils.hasText(embeddingModel)) {
            return embeddingModel.trim();
        }
        return embeddingProperties.getModel();
    }

    private String serializeVector(List<Double> vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(50000, "embedding vector serialization failed");
        }
    }

    private String resolveDocumentEmbeddingStatus(
            int total, int successCount, int failedCount, int pendingCount, int processingCount) {
        if (total == 0) {
            return DOCUMENT_STATUS_NOT_CHUNKED;
        }
        if (successCount == total) {
            return DOCUMENT_STATUS_SUCCESS;
        }
        if (failedCount == total) {
            return DOCUMENT_STATUS_FAILED;
        }
        if (processingCount > 0) {
            return DOCUMENT_STATUS_PROCESSING;
        }
        if (pendingCount == total) {
            return DOCUMENT_STATUS_PENDING;
        }
        return DOCUMENT_STATUS_PARTIAL;
    }

    private String truncateError(String errorMessage) {
        String error = StringUtils.hasText(errorMessage)
                ? LogSanitizer.sanitize(errorMessage, MAX_ERROR_MESSAGE_LENGTH).trim()
                : "Document embedding failed";
        if (error.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return error;
        }
        return error.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String safeRootCauseMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        String message = throwable.getMessage();
        if (!StringUtils.hasText(message)) {
            return throwable.getClass().getName();
        }
        return LogSanitizer.safeMessage(message);
    }

    private ChunkEmbeddingStatusVO toStatusVO(DocumentChunk chunk, ChunkEmbedding embedding, String status) {
        return ChunkEmbeddingStatusVO.builder()
                .chunkId(chunk.getId())
                .chunkIndex(chunk.getChunkIndex())
                .embeddingModel(embedding == null ? null : embedding.getEmbeddingModel())
                .vectorId(embedding == null ? null : embedding.getVectorId())
                .status(status)
                .embeddingError(embedding == null ? null : embedding.getEmbeddingError())
                .embeddedAt(embedding == null ? null : embedding.getEmbeddedAt())
                .build();
    }

    private record EmbeddedChunkResult(DocumentChunk chunk, EmbeddingResult result) {
    }
}
