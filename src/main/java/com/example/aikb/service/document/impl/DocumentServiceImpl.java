package com.example.aikb.service.document.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aikb.common.PageResult;
import com.example.aikb.dto.document.DocumentFileUploadRequest;
import com.example.aikb.dto.document.DocumentListQuery;
import com.example.aikb.dto.document.DocumentUploadRequest;
import com.example.aikb.entity.Document;
import com.example.aikb.entity.KnowledgeBase;
import com.example.aikb.entity.TaskRecord;
import com.example.aikb.exception.BusinessException;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 文档业务服务实现类，按当前登录用户隔离知识库和文档数据。
 */
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private static final String PARSE_STATUS_UPLOADED = "UPLOADED";
    private static final String PARSE_STATUS_PENDING = "PENDING";
    private static final String TASK_TYPE_DOCUMENT_PARSE = "DOCUMENT_PARSE";
    private static final String BIZ_TYPE_DOCUMENT = "DOCUMENT";
    private static final String TASK_STATUS_PENDING = "PENDING";
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> SUPPORTED_FILE_TYPES = Set.of("pdf", "doc", "docx", "txt", "md");

    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final TaskRecordMapper taskRecordMapper;
    private final LocalDocumentStorage localDocumentStorage;

    /**
     * 上传文档元数据，并绑定到当前用户拥有的知识库。
     *
     * @param request 文档上传请求参数
     * @return 文档详情
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
        document.setStoragePath(resolveStoragePath(request.getStoragePath(), request.getKnowledgeBaseId(), fileName));
        document.setParseStatus(PARSE_STATUS_UPLOADED);
        document.setCreatedBy(userId);

        int rows = documentMapper.insert(document);
        if (rows != 1 || document.getId() == null) {
            throw new BusinessException("文档上传失败");
        }

        return toDetailVO(documentMapper.selectById(document.getId()));
    }

    /**
     * 上传真实文件，上传成功仅表示文件已保存并生成 UPLOADED 文档记录，不代表已经解析。
     *
     * @param request 真实文件上传请求参数
     * @return 上传结果
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
     * 注册事务回滚后的文件清理，覆盖 SQL 已执行但事务提交失败的场景。
     *
     * @param storedFile 已保存文件信息
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
     *
     * @param query 文档分页查询参数
     * @return 文档分页结果
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

    /**
     * 查询当前用户可访问的文档详情。
     *
     * @param id 文档 ID
     * @return 文档详情
     */
    @Override
    public DocumentDetailVO getById(Long id) {
        Long userId = CurrentUser.getUserId();
        Document document = getOwnDocument(id, userId);
        return toDetailVO(document);
    }

    /**
     * 为当前用户可访问的文档创建解析任务记录。
     *
     * @param id 文档 ID
     * @return 解析任务 ID
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

        int rows = taskRecordMapper.insert(taskRecord);
        if (rows != 1 || taskRecord.getId() == null) {
            throw new BusinessException("解析任务创建失败");
        }
        document.setParseStatus(PARSE_STATUS_PENDING);
        documentMapper.updateById(document);
        return taskRecord.getId();
    }

    /**
     * 归一化并校验文件类型和大小，避免 MVP 阶段写入明显不可解析或过大的文档。
     *
     * @param fileType 文件类型
     * @param fileSize 文件大小，单位字节
     * @return 归一化后的文件类型
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
     *
     * @param knowledgeBaseId 知识库 ID
     * @param userId 当前用户 ID
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
     *
     * @param id 文档 ID
     * @param userId 当前用户 ID
     * @return 文档实体
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
     * 解析文件存储路径，MVP 阶段没有真实文件存储时生成占位路径。
     *
     * @param storagePath 请求中的存储路径
     * @param knowledgeBaseId 知识库 ID
     * @param fileName 文件名
     * @return 最终存储路径
     */
    private String resolveStoragePath(String storagePath, Long knowledgeBaseId, String fileName) {
        if (StringUtils.hasText(storagePath)) {
            return storagePath.trim();
        }
        return "kb/" + knowledgeBaseId + "/" + fileName;
    }

    /**
     * 将文档实体转换为列表展示对象。
     *
     * @param document 文档实体
     * @return 文档列表展示对象
     */
    private DocumentListVO toListVO(Document document) {
        return DocumentListVO.builder()
                .id(document.getId())
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
     *
     * @param document 文档实体
     * @return 文档详情展示对象
     */
    private DocumentDetailVO toDetailVO(Document document) {
        return DocumentDetailVO.builder()
                .id(document.getId())
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
     *
     * @param document 文档实体
     * @return 真实文件上传响应对象
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
