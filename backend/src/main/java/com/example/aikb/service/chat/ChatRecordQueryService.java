package com.example.aikb.service.chat;

import com.example.aikb.common.PageResult;
import com.example.aikb.vo.chat.ChatRecordDetailVO;
import com.example.aikb.vo.chat.ChatRecordListItemVO;

/**
 * 历史问答记录查询服务，只允许查询当前登录用户自己的记录。
 */
public interface ChatRecordQueryService {

    /**
     * 分页查询当前用户的历史问答记录。
     *
     * @param knowledgeBaseId 可选知识库过滤条件
     * @param pageNum 当前页码，从1开始
     * @param pageSize 每页条数
     * @return 问答记录分页结果
     */
    PageResult<ChatRecordListItemVO> page(Long knowledgeBaseId, long pageNum, long pageSize);

    /**
     * 查询当前用户指定问答记录详情。
     *
     * @param id 问答记录ID
     * @return 问答记录详情
     */
    ChatRecordDetailVO getById(Long id);
}
