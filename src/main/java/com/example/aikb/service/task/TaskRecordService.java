package com.example.aikb.service.task;

import com.example.aikb.entity.Document;
import com.example.aikb.entity.TaskRecord;
import com.example.aikb.vo.task.TaskRecordVO;
import java.util.List;

/**
 * 任务记录服务，负责文档处理任务的状态流转和权限内查询。
 */
public interface TaskRecordService {

    /**
     * 创建文档处理任务，初始状态为 PENDING。
     *
     * @param document 文档实体
     * @param userId 当前用户 ID
     * @return 已创建的任务记录
     */
    TaskRecord createDocumentProcessTask(Document document, Long userId);

    /**
     * 将任务标记为处理中。
     *
     * @param taskId 任务 ID
     */
    void markProcessing(Long taskId);

    /**
     * 将任务标记为成功。
     *
     * @param taskId 任务 ID
     */
    void markSuccess(Long taskId);

    /**
     * 将任务标记为失败，并保存错误信息。
     *
     * @param taskId 任务 ID
     * @param errorMessage 错误信息
     */
    void markFailed(Long taskId, String errorMessage);

    /**
     * 查询当前用户可访问的任务详情。
     *
     * @param taskId 任务 ID
     * @return 任务详情
     */
    TaskRecordVO getById(Long taskId);

    /**
     * 查询当前用户可访问文档的最近任务列表。
     *
     * @param documentId 文档 ID
     * @return 最近任务列表
     */
    List<TaskRecordVO> listByDocument(Long documentId);
}
