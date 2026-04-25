package com.example.aikb.service.document.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aikb.common.PageResult;
import com.example.aikb.dto.document.DocumentFileUploadRequest;
import com.example.aikb.dto.document.DocumentListQuery;
import com.example.aikb.dto.document.DocumentUploadRequest;
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
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.document.DocumentService;
import com.example.aikb.service.document.LocalDocumentStorage;
import com.example.aikb.service.document.LocalDocumentStorage.StoredDocumentFile;
import com.example.aikb.vo.document.DocumentDetailVO;
import com.example.aikb.vo.document.DocumentFileUploadVO;
import com.example.aikb.vo.document.DocumentListVO;
import com.example.aikb.vo.document.DocumentStatusVO;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 文档服务实现，按当前登录用户隔离知识库和文档数据。
 */
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final String PARSE_STATUS_UPLOADED = "UPLOADED";
    private static final String PARSE_STATUS_PENDING = "PENDING";
    private static final String TASK_TYPE_DOCUMENT_PARSE = "DOCUMENT_PARSE";
    private static final String TASK_TYPE_DOCUMENT_PROCESS = "DOCUMENT_PROCESS";
    private static final String TASK_TYPE_DOCUMENT_EMBEDDING = "DOCUMENT_EMBEDDING";
    private static final String BIZ_TYPE_DOCUMENT = "DOCUMENT";
    private static final String TASK_STATUS_PENDING = "PENDING";
    private static final String EMBEDDING_STATUS_SUCCESS = "SUCCESS";
    private static final String EMBEDDING_STATUS_FAILED = "FAILED";
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> SUPPORTED_FILE_TYPES = Set.of("pdf", "doc", "docx", "txt", "md");

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final ChunkEmbeddingMapper chunkEmbeddingMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final TaskRecordMapper taskRecordMapper;
    private final LocalDocumentStorage localDocumentStorage;

    /**
     * 上传文档元数据，并绑定到当前用户拥有的知识库。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentDetailVO upload(DocumentUploadRequest request) {
        Long userId = CurrentUser.getUserId();
        ensureOwnKnowledgeBase(request.getKnowledgeBaseId(), userId);

        String fileName = request.getFileName().trim();
        String fileType = normalizeAndValidateFile(request.getFileType(), request.getFileSize());

        Document document = new Document();
        document.setKnowledgeBaseId(request.getKnowledgeBaseId());
        document.setFileName(fileName);
        document.setFileType(fileType);
        document.setFileSize(request.getFileSize());
        document.setStoragePath(resolveMetadataStoragePath(request.getKnowledgeBaseId(), fileName));
        document.setParseStatus(PARSE_STATUS_UPLOADED);
        document.setCreatedBy(userId);

        int rows = documentMapper.insert(document);
        if (rows != 1 || document.getId() == null) {
            throw new BusinessException("文档上传失败");
        }

        return toDetailVO(documentMapper.selectById(document.getId()));
    }

    /**
     * 上传真实文件，并写入文档记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentFileUploadVO uploadFile(DocumentFileUploadRequest request) {
        Long userId = CurrentUser.getUserId();
        ensureOwnKnowledgeBase(request.getKnowledgeBaseId(), userId);

        StoredDocumentFile storedFile = localDocumentStorage.save(
                userId,
                request.getKnowledgeBaseId(),
                request.getFile(),
                request.getFileName());
        registerRollbackCleanup(storedFile);
        try {
            Document document = new Document();
            document.setKnowledgeBaseId(request.getKnowledgeBaseId());
            document.setFileName(storedFile.getFileName());
            document.setFileType(storedFile.getFileType());
            document.setFileSize(storedFile.getFileSize());
            document.setStoragePath(storedFile.getStoragePath());
            document.setParseStatus(PARSE_STATUS_UPLOADED);
            document.setCreatedBy(userId);

            int rows = documentMapper.insert(document);
            if (rows != 1 || document.getId() == null) {
                throw new BusinessException("真实文件上传记录保存失败");
            }
            return toFileUploadVO(documentMapper.selectById(document.getId()));
        } catch (RuntimeException ex) {
            localDocumentStorage.deleteQuietly(storedFile);
            throw ex;
        }
    }

    /**
     * 注册事务回滚后的文件清理，避免数据库回滚后残留孤儿文件。
     */
    private void registerRollbackCleanup(StoredDocumentFile storedFile) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    localDocumentStorage.deleteQuietly(storedFile);
                }
            }
        });
    }

    /**
     * 分页查询当前用户可访问的文档列表，可按知识库筛选。
     */
    @Override
    public PageResult<DocumentListVO> page(DocumentListQuery query) {
        Long userId = CurrentUser.getUserId();
        if (query.getKnowledgeBaseId() != null) {
            ensureOwnKnowledgeBase(query.getKnowledgeBaseId(), userId);
        }

        Page<Document> page = Page.of(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<Document>()
                .eq(Document::getCreatedBy, userId)
                .eq(query.getKnowledgeBaseId() != null, Document::getKnowledgeBaseId, query.getKnowledgeBaseId())
                .orderByDesc(Document::getCreatedAt);

        IPage<Document> result = documentMapper.selectPage(page, wrapper);
        List<DocumentListVO> list = result.getRecords()
                .stream()
                .map(this::toListVO)
                .toList();

        return PageResult.<DocumentListVO>builder()
                .list(list)
                .total(result.getTotal())
                .pageNum(query.getPageNum())
                .pageSize(query.getPageSize())
                .build();
    }

    @Override
    public List<DocumentListVO> listByKnowledgeBase(Long knowledgeBaseId) {
        Long userId = CurrentUser.getUserId();
        ensureOwnKnowledgeBase(knowledgeBaseId, userId);

        return documentMapper.selectList(new LambdaQueryWrapper<Document>()
                        .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByDesc(Document::getCreatedAt))
                .stream()
                .map(this::toListVO)
                .toList();
    }

    /**
     * 查询当前用户可访问的文档详情。
     */
    @Override
    public DocumentDetailVO getById(Long id) {
        Long userId = CurrentUser.getUserId();
        Document document = getOwnDocument(id, userId);
        return toDetailVO(document);
    }

    /**
     * 查询文档状态概览。
     * 当前实现只做轻量统计，不再复用 embedding 明细接口，避免状态查询接口被大文档拖慢。
     */
    @Override
    public DocumentStatusVO getStatus(Long id) {
        Long userId = CurrentUser.getUserId();
        Document document = getOwnDocument(id, userId);
        int chunkCount = Math.toIntExact(documentChunkMapper.selectCount(new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getDocumentId, document.getId())));
        int embeddingSuccessCount = Math.toIntExact(chunkEmbeddingMapper.selectCount(new LambdaQueryWrapper<ChunkEmbedding>()
                .eq(ChunkEmbedding::getDocumentId, document.getId())
                .eq(ChunkEmbedding::getStatus, EMBEDDING_STATUS_SUCCESS)));
        int embeddingFailedCount = Math.toIntExact(chunkEmbeddingMapper.selectCount(new LambdaQueryWrapper<ChunkEmbedding>()
                .eq(ChunkEmbedding::getDocumentId, document.getId())
                .eq(ChunkEmbedding::getStatus, EMBEDDING_STATUS_FAILED)));
        TaskRecord latestTask = findLatestStatusTask(document.getId());

        return DocumentStatusVO.builder()
                .documentId(document.getId())
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .parseStatus(document.getParseStatus())
                .chunkCount(chunkCount)
                // 当前阶段直接使用实时切片总数作为 embeddingTotal 的占位统计。
                .embeddingTotal(chunkCount)
                .embeddingSuccessCount(embeddingSuccessCount)
                .embeddingFailedCount(embeddingFailedCount)
                .latestTaskStatus(latestTask == null ? null : latestTask.getStatus())
                .latestErrorMessage(latestTask == null ? null : latestTask.getErrorMessage())
                .build();
    }

    /**
     * 为当前用户可访问的文档创建解析任务记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createParseTask(Long id) {
        Long userId = CurrentUser.getUserId();
        Document document = getOwnDocument(id, userId);

        TaskRecord pendingTask = taskRecordMapper.selectOne(new LambdaQueryWrapper<TaskRecord>()
                .eq(TaskRecord::getTaskType, TASK_TYPE_DOCUMENT_PARSE)
                .eq(TaskRecord::getBizType, BIZ_TYPE_DOCUMENT)
                .eq(TaskRecord::getBizId, document.getId())
                .eq(TaskRecord::getStatus, TASK_STATUS_PENDING)
                .last("LIMIT 1"));
        if (pendingTask != null) {
            return pendingTask.getId();
        }

        TaskRecord taskRecord = new TaskRecord();
        taskRecord.setTaskType(TASK_TYPE_DOCUMENT_PARSE);
        taskRecord.setBizType(BIZ_TYPE_DOCUMENT);
        taskRecord.setBizId(document.getId());
        taskRecord.setStatus(TASK_STATUS_PENDING);
        taskRecord.setRetryCount(0);
        taskRecord.setCreatedBy(userId);

        int rows = taskRecordMapper.insert(taskRecord);
        if (rows != 1 || taskRecord.getId() == null) {
            throw new BusinessException("解析任务创建失败");
        }
        document.setParseStatus(PARSE_STATUS_PENDING);
        documentMapper.updateById(document);
        return taskRecord.getId();
    }

    /**
     * 归一化并校验文件类型和大小。
     */
    private String normalizeAndValidateFile(String fileType, Long fileSize) {
        String normalizedFileType = fileType.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_FILE_TYPES.contains(normalizedFileType)) {
            throw new BusinessException("暂不支持该文件类型，仅支持 pdf、doc、docx、txt、md");
        }
        if (fileSize > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过20MB");
        }
        return normalizedFileType;
    }

    /**
     * 校验知识库是否属于当前用户。
     */
    private void ensureOwnKnowledgeBase(Long knowledgeBaseId, Long userId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectOne(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getId, knowledgeBaseId)
                .eq(KnowledgeBase::getOwnerId, userId)
                .eq(KnowledgeBase::getStatus, 1)
                .last("LIMIT 1"));
        if (knowledgeBase == null) {
            throw new BusinessException(40400, "知识库不存在");
        }
    }

    /**
     * 查询并校验文档是否属于当前用户自己的知识库。
     */
    private Document getOwnDocument(Long id, Long userId) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException(40400, "文档不存在");
        }
        ensureOwnKnowledgeBase(document.getKnowledgeBaseId(), userId);
        return document;
    }

    /**
     * 状态概览优先展示最近真正执行过的任务；若暂无执行任务，再回退到最近创建的占位任务。
     */
    private TaskRecord findLatestStatusTask(Long documentId) {
        TaskRecord latestExecutionTask = taskRecordMapper.selectOne(new LambdaQueryWrapper<TaskRecord>()
                .eq(TaskRecord::getBizType, BIZ_TYPE_DOCUMENT)
                .eq(TaskRecord::getBizId, documentId)
                .in(TaskRecord::getTaskType, TASK_TYPE_DOCUMENT_PROCESS, TASK_TYPE_DOCUMENT_EMBEDDING)
                .orderByDesc(TaskRecord::getStartedAt)
                .orderByDesc(TaskRecord::getCreatedAt)
                .last("LIMIT 1"));
        if (latestExecutionTask != null) {
            return latestExecutionTask;
        }
        return taskRecordMapper.selectOne(new LambdaQueryWrapper<TaskRecord>()
                .eq(TaskRecord::getBizType, BIZ_TYPE_DOCUMENT)
                .eq(TaskRecord::getBizId, documentId)
                .orderByDesc(TaskRecord::getCreatedAt)
                .last("LIMIT 1"));
    }

    /**
     * 为兼容元数据上传接口生成占位路径。
     */
    private String resolveMetadataStoragePath(Long knowledgeBaseId, String fileName) {
        return "metadata/" + knowledgeBaseId + "/" + fileName;
    }

    /**
     * 将文档实体转换为列表展示对象。
     */
    private DocumentListVO toListVO(Document document) {
        return DocumentListVO.builder()
                .id(document.getId())
                .documentId(document.getId())
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .parseStatus(document.getParseStatus())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    /**
     * 将文档实体转换为详情展示对象。
     */
    private DocumentDetailVO toDetailVO(Document document) {
        return DocumentDetailVO.builder()
                .id(document.getId())
                .documentId(document.getId())
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .storagePath(document.getStoragePath())
                .parseStatus(document.getParseStatus())
                .createdBy(document.getCreatedBy())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }

    /**
     * 将文档实体转换为真实文件上传响应对象。
     */
    private DocumentFileUploadVO toFileUploadVO(Document document) {
        return DocumentFileUploadVO.builder()
                .id(document.getId())
                .knowledgeBaseId(document.getKnowledgeBaseId())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .storagePath(document.getStoragePath())
                .parseStatus(document.getParseStatus())
                .createdBy(document.getCreatedBy())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
