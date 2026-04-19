package com.example.aikb.service.chat;

import com.example.aikb.common.PageResult;
import com.example.aikb.vo.chat.MissedQuestionItemVO;

/**
 * 未命中问题查询服务，基于 chat_record.matched=false 查询。
 */
public interface MissedQuestionQueryService {

    /**
     * 分页查询未命中的问答记录。
     *
     * @param knowledgeBaseId 可选知识库过滤条件
     * @param pageNum 当前页码，从1开始
     * @param pageSize 每页条数
     * @return 未命中问题分页结果
     */
    PageResult<MissedQuestionItemVO> page(Long knowledgeBaseId, long pageNum, long pageSize);
}
