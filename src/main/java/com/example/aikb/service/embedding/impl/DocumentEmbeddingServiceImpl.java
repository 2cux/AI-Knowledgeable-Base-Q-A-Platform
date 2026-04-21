package com.example.aikb.service.embedding.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.embedding.DocumentEmbeddingService;
import com.example.aikb.service.embedding.EmbeddingClient;
import com.example.aikb.service.embedding.EmbeddingResult;
import com.example.aikb.service.task.TaskRecordService;
import com.example.aikb.vo.document.ChunkEmbeddingStatusVO;
import com.example.aikb.vo.document.DocumentEmbeddingStatusVO;
import com.example.aikb.vo.document.DocumentEmbeddingVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 文档向量化服务实现类，负责将 document_chunk 同步到向量化状态表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentEmbeddingServiceImpl implements DocumentEmbeddingService {

    private static final String DEFAULT_EMBEDDING_MODEL = "local-hash-embedding-v1";
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
    private static final String DOCUMENT_STATUS_PARTIAL = "PARTIAL";

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final ChunkEmbeddingMapper chunkEmbeddingMapper;
    private final EmbeddingClient embeddingClient;
    private final TaskRecordService taskRecordService;
    private final ObjectMapper objectMapper;

    /**
     * 对当前用户可访问的指定文档执行向量化。
     *
     * @param documentId 文档 ID
     * @param request 文档向量化请求参数
     * @return 文档向量化执行结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentEmbeddingVO embedDocument(Long documentId, DocumentEmbeddingRequest request) {
        Long userId = CurrentUser.getUserId();
        Document document = getOwnDocument(documentId, userId);
        TaskRecord taskRecord = taskRecordService.createDocumentEmbeddingTask(document, userId);
        taskRecordService.markProcessing(taskRecord.getId());

        try {
            return doEmbedDocument(document, request, taskRecord.getId());
        } catch (RuntimeException ex) {
            taskRecordService.markFailed(taskRecord.getId(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * 执行文档向量化主体流程。
     */
    private DocumentEmbeddingVO doEmbedDocument(Document document, DocumentEmbeddingRequest request, Long taskId) {
        List<DocumentChunk> chunks = listEmbeddableDocumentChunks(document.getId());
        if (chunks.isEmpty()) {
            throw new BusinessException("文档尚未切片，请先执行文档处理");
        }

        String embeddingModel = resolveEmbeddingModel(request);
        int successCount = 0;
        int failedCount = 0;

        for (DocumentChunk chunk : chunks) {
            ChunkEmbedding embedding = null;
            try {
                embedding = findOrCreateEmbedding(document, chunk, embeddingModel);
                markProcessing(embedding, embeddingModel);
                EmbeddingResult result = embeddingClient.embed(chunk.getId(), chunk.getContent(), embeddingModel);
                markSuccess(embedding, result);
                successCount++;
            } catch (RuntimeException ex) {
                log.warn("Embedding failed, documentId={}, chunkId={}, model={}, error={}",
                        document.getId(), chunk.getId(), embeddingModel, ex.getMessage());
                if (embedding != null) {
                    markFailed(embedding, embeddingModel, ex.getMessage());
                }
                failedCount++;
            }
        }

        String taskStatus = STATUS_SUCCESS;
        if (failedCount > 0) {
            taskStatus = STATUS_FAILED;
            taskRecordService.markFailed(taskId,
                    "文档向量化失败chunk数量：" + failedCount + "/" + chunks.size());
        } else {
            taskRecordService.markSuccess(taskId);
        }

        return DocumentEmbeddingVO.builder()
                .documentId(document.getId())
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .total(chunks.size())
                .successCount(successCount)
                .failedCount(failedCount)
                .embeddingModel(embeddingModel)
                .taskId(taskId)
                .taskStatus(taskStatus)
                .build();
    }

    /**
     * 查询当前用户可访问的指定文档向量化状态。
     *
     * @param documentId 文档 ID
     * @return 文档向量化状态汇总
     */
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

    /**
     * 查询文档下的所有 chunk，并按 chunkIndex 保持稳定顺序。
     *
     * @param documentId 文档 ID
     * @return 文档切片列表
     */
    private List<DocumentChunk> listDocumentChunks(Long documentId) {
        return documentChunkMapper.selectList(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, documentId)
                .orderByAsc(DocumentChunk::getChunkIndex));
    }

    /**
     * 查询文档下可向量化的非空 chunk。
     *
     * @param documentId 文档 ID
     * @return 非空切片列表
     */
    private List<DocumentChunk> listEmbeddableDocumentChunks(Long documentId) {
        return listDocumentChunks(documentId)
                .stream()
                .filter(chunk -> StringUtils.hasText(chunk.getContent()))
                .toList();
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
     * 解析请求中的模型名称，不传时使用 MVP 默认占位模型。
     *
     * @param request 文档向量化请求
     * @return 向量化模型名称
     */
    private String resolveEmbeddingModel(DocumentEmbeddingRequest request) {
        if (request != null && StringUtils.hasText(request.getEmbeddingModel())) {
            return request.getEmbeddingModel().trim();
        }
        return DEFAULT_EMBEDDING_MODEL;
    }

    /**
     * 查询已有状态记录；不存在则创建 PENDING 记录。
     *
     * @param document 文档实体
     * @param chunk 文档切片实体
     * @param embeddingModel 向量化模型名称
     * @return 切片向量化状态实体
     */
    private ChunkEmbedding findOrCreateEmbedding(Document document, DocumentChunk chunk, String embeddingModel) {
        ChunkEmbedding embedding = chunkEmbeddingMapper.selectOne(new LambdaQueryWrapper<ChunkEmbedding>()
                .eq(ChunkEmbedding::getChunkId, chunk.getId())
                .last("LIMIT 1"));
        if (embedding != null) {
            embedding.setEmbeddingModel(embeddingModel);
            embedding.setStatus(STATUS_PENDING);
            chunkEmbeddingMapper.update(null, new LambdaUpdateWrapper<ChunkEmbedding>()
                    .eq(ChunkEmbedding::getId, embedding.getId())
                    .set(ChunkEmbedding::getEmbeddingModel, embeddingModel)
                    .set(ChunkEmbedding::getStatus, STATUS_PENDING)
                    .set(ChunkEmbedding::getVectorId, null)
                    .set(ChunkEmbedding::getVectorJson, null)
                    .set(ChunkEmbedding::getEmbeddingError, null)
                    .set(ChunkEmbedding::getEmbeddedAt, null));
            return embedding;
        }

        embedding = new ChunkEmbedding();
        embedding.setDocumentId(document.getId());
        embedding.setKnowledgeBaseId(document.getKnowledgeBaseId());
        embedding.setChunkId(chunk.getId());
        embedding.setEmbeddingModel(embeddingModel);
        embedding.setStatus(STATUS_PENDING);
        chunkEmbeddingMapper.insert(embedding);
        return embedding;
    }

    /**
     * 标记单个 chunk 向量化成功。
     *
     * @param embedding 切片向量化状态实体
     * @param result 向量化结果
     */
    private void markSuccess(ChunkEmbedding embedding, EmbeddingResult result) {
        if (result.getVector() == null || result.getVector().isEmpty()) {
            throw new BusinessException("embedding向量为空");
        }
        embedding.setEmbeddingModel(result.getEmbeddingModel());
        embedding.setVectorId(result.getVectorId());
        embedding.setVectorJson(serializeVector(result.getVector()));
        embedding.setStatus(STATUS_SUCCESS);
        embedding.setEmbeddingError(null);
        embedding.setEmbeddedAt(LocalDateTime.now());
        chunkEmbeddingMapper.update(null, new LambdaUpdateWrapper<ChunkEmbedding>()
                .eq(ChunkEmbedding::getId, embedding.getId())
                .set(ChunkEmbedding::getEmbeddingModel, result.getEmbeddingModel())
                .set(ChunkEmbedding::getVectorId, result.getVectorId())
                .set(ChunkEmbedding::getVectorJson, embedding.getVectorJson())
                .set(ChunkEmbedding::getStatus, STATUS_SUCCESS)
                .set(ChunkEmbedding::getEmbeddingError, null)
                .set(ChunkEmbedding::getEmbeddedAt, embedding.getEmbeddedAt()));
    }

    /**
     * 标记单个 chunk 正在向量化。
     *
     * @param embedding 切片向量化状态实体
     * @param embeddingModel 向量化模型名称
     */
    private void markProcessing(ChunkEmbedding embedding, String embeddingModel) {
        embedding.setEmbeddingModel(embeddingModel);
        embedding.setStatus(STATUS_PROCESSING);
        embedding.setVectorId(null);
        embedding.setVectorJson(null);
        embedding.setEmbeddingError(null);
        embedding.setEmbeddedAt(null);
        chunkEmbeddingMapper.update(null, new LambdaUpdateWrapper<ChunkEmbedding>()
                .eq(ChunkEmbedding::getId, embedding.getId())
                .set(ChunkEmbedding::getEmbeddingModel, embeddingModel)
                .set(ChunkEmbedding::getVectorId, null)
                .set(ChunkEmbedding::getVectorJson, null)
                .set(ChunkEmbedding::getStatus, STATUS_PROCESSING)
                .set(ChunkEmbedding::getEmbeddingError, null)
                .set(ChunkEmbedding::getEmbeddedAt, null));
    }

    /**
     * 标记单个 chunk 向量化失败。
     *
     * @param embedding 切片向量化状态实体
     * @param embeddingModel 向量化模型名称
     * @param errorMessage 失败原因
     */
    private void markFailed(ChunkEmbedding embedding, String embeddingModel, String errorMessage) {
        embedding.setEmbeddingModel(embeddingModel);
        embedding.setStatus(STATUS_FAILED);
        embedding.setEmbeddingError(truncateError(errorMessage));
        chunkEmbeddingMapper.update(null, new LambdaUpdateWrapper<ChunkEmbedding>()
                .eq(ChunkEmbedding::getId, embedding.getId())
                .set(ChunkEmbedding::getEmbeddingModel, embeddingModel)
                .set(ChunkEmbedding::getVectorId, null)
                .set(ChunkEmbedding::getVectorJson, null)
                .set(ChunkEmbedding::getStatus, STATUS_FAILED)
                .set(ChunkEmbedding::getEmbeddingError, embedding.getEmbeddingError())
                .set(ChunkEmbedding::getEmbeddedAt, null));
    }

    /**
     * 序列化向量，供后续真实检索模块使用。
     */
    private String serializeVector(List<Double> vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(50000, "embedding向量序列化失败");
        }
    }

    /**
     * 计算文档整体向量化状态。
     *
     * @param total 总 chunk 数
     * @param successCount 成功数量
     * @param failedCount 失败数量
     * @param pendingCount 未处理数量
     * @param processingCount 处理中数量
     * @return 文档整体向量化状态
     */
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

    /**
     * 截断错误信息，避免超过数据库字段长度导致状态回写失败。
     *
     * @param errorMessage 原始错误信息
     * @return 可安全入库的错误信息
     */
    private String truncateError(String errorMessage) {
        String error = errorMessage == null ? "向量化失败" : errorMessage;
        if (error.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return error;
        }
        return error.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    /**
     * 将 chunk 和向量化状态转换为展示对象。
     *
     * @param chunk 文档切片实体
     * @param embedding 切片向量化状态实体，可能为空
     * @param status 当前状态
     * @return 切片向量化状态展示对象
     */
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
}
