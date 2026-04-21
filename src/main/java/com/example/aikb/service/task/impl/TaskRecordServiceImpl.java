package com.example.aikb.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aikb.entity.Document;
import com.example.aikb.entity.KnowledgeBase;
import com.example.aikb.entity.TaskRecord;
import com.example.aikb.exception.BusinessException;
import com.example.aikb.mapper.DocumentMapper;
import com.example.aikb.mapper.KnowledgeBaseMapper;
import com.example.aikb.mapper.TaskRecordMapper;
import com.example.aikb.security.CurrentUser;
import com.example.aikb.service.task.TaskRecordService;
import com.example.aikb.vo.task.TaskRecordVO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 任务记录服务实现，当前优先支撑同步文档处理任务的状态闭环。
 */
@Service
@RequiredArgsConstructor
public class TaskRecordServiceImpl implements TaskRecordService {

    private static final String TASK_TYPE_DOCUMENT_PROCESS = "DOCUMENT_PROCESS";
    private static final String TASK_TYPE_DOCUMENT_EMBEDDING = "DOCUMENT_EMBEDDING";
    private static final String BIZ_TYPE_DOCUMENT = "DOCUMENT";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;
    private static final int RECENT_TASK_LIMIT = 20;

    private final TaskRecordMapper taskRecordMapper;
    private final DocumentMapper documentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * 创建文档处理任务；使用新事务保证后续处理失败时任务仍可追踪。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public TaskRecord createDocumentProcessTask(Document document, Long userId) {
        return createDocumentTask(document, userId, TASK_TYPE_DOCUMENT_PROCESS);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public TaskRecord createDocumentEmbeddingTask(Document document, Long userId) {
        return createDocumentTask(document, userId, TASK_TYPE_DOCUMENT_EMBEDDING);
    }

    /**
     * 创建指定类型的文档任务。
     */
    private TaskRecord createDocumentTask(Document document, Long userId, String taskType) {
        TaskRecord taskRecord = new TaskRecord();
        taskRecord.setTaskType(taskType);
        taskRecord.setBizType(BIZ_TYPE_DOCUMENT);
        taskRecord.setBizId(document.getId());
        taskRecord.setStatus(STATUS_PENDING);
        taskRecord.setRetryCount(0);
        taskRecord.setCreatedBy(userId);

        int rows = taskRecordMapper.insert(taskRecord);
        if (rows != 1 || taskRecord.getId() == null) {
            throw new BusinessException("文档处理任务创建失败");
        }
        return taskRecord;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markProcessing(Long taskId) {
        TaskRecord update = new TaskRecord();
        update.setId(taskId);
        update.setStatus(STATUS_PROCESSING);
        update.setStartedAt(LocalDateTime.now());
        updateTask(update, "文档处理任务状态更新失败");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markSuccess(Long taskId) {
        TaskRecord update = new TaskRecord();
        update.setId(taskId);
        update.setStatus(STATUS_SUCCESS);
        update.setFinishedAt(LocalDateTime.now());
        updateTask(update, "文档处理任务完成状态更新失败");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markFailed(Long taskId, String errorMessage) {
        TaskRecord update = new TaskRecord();
        update.setId(taskId);
        update.setStatus(STATUS_FAILED);
        update.setErrorMessage(truncateErrorMessage(errorMessage));
        update.setFinishedAt(LocalDateTime.now());
        updateTask(update, "文档处理任务失败状态更新失败");
    }

    @Override
    public TaskRecordVO getById(Long taskId) {
        Long userId = CurrentUser.getUserId();
        TaskRecord taskRecord = taskRecordMapper.selectById(taskId);
        if (taskRecord == null) {
            throw new BusinessException(40400, "任务不存在");
        }
        ensureOwnDocumentTask(taskRecord, userId);
        return toVO(taskRecord);
    }

    @Override
    public List<TaskRecordVO> listByDocument(Long documentId) {
        Long userId = CurrentUser.getUserId();
        Document document = getOwnDocument(documentId, userId);
        return taskRecordMapper.selectList(new LambdaQueryWrapper<TaskRecord>()
                        .eq(TaskRecord::getBizType, BIZ_TYPE_DOCUMENT)
                        .eq(TaskRecord::getBizId, document.getId())
                        .orderByDesc(TaskRecord::getCreatedAt)
                        .last("LIMIT " + RECENT_TASK_LIMIT))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 更新任务记录并校验影响行数。
     */
    private void updateTask(TaskRecord update, String errorMessage) {
        int rows = taskRecordMapper.updateById(update);
        if (rows != 1) {
            throw new BusinessException(errorMessage);
        }
    }

    /**
     * 校验任务是否属于当前用户可访问的文档。
     */
    private void ensureOwnDocumentTask(TaskRecord taskRecord, Long userId) {
        if (!BIZ_TYPE_DOCUMENT.equals(taskRecord.getBizType())) {
            throw new BusinessException(40400, "任务不存在");
        }
        getOwnDocument(taskRecord.getBizId(), userId);
    }

    /**
     * 查询并校验文档是否属于当前用户自己的知识库。
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
     * 防止超过 task_record.error_message 字段长度。
     */
    private String truncateErrorMessage(String errorMessage) {
        String message = StringUtils.hasText(errorMessage) ? errorMessage.trim() : "文档处理失败";
        if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    /**
     * 转换任务记录展示对象。
     */
    private TaskRecordVO toVO(TaskRecord taskRecord) {
        return TaskRecordVO.builder()
                .id(taskRecord.getId())
                .documentId(taskRecord.getBizId())
                .taskType(taskRecord.getTaskType())
                .status(taskRecord.getStatus())
                .errorMessage(taskRecord.getErrorMessage())
                .startedAt(taskRecord.getStartedAt())
                .finishedAt(taskRecord.getFinishedAt())
                .createdBy(taskRecord.getCreatedBy())
                .createdAt(taskRecord.getCreatedAt())
                .updatedAt(taskRecord.getUpdatedAt())
                .build();
    }
}
