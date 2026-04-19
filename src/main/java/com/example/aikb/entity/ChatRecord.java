package com.example.aikb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 问答记录实体，保存一次用户提问、生成答案、检索命中情况和引用来源。
 */
@Data
@TableName("chat_record")
public class ChatRecord {

    /** 问答记录主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提问用户ID。 */
    @TableField("user_id")
    private Long userId;

    /** 本次提问所属知识库ID。 */
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    /** 会话ID，用于轻量多轮追问。 */
    @TableField("conversation_id")
    private String conversationId;

    /** 用户原始问题。 */
    private String question;

    /** 生成的答案。 */
    private String answer;

    /** 是否检索到相关切片。 */
    private Boolean matched;

    /** 实际命中的切片数量。 */
    @TableField("retrieved_chunk_count")
    private Integer retrievedChunkCount;

    /** 本次检索请求的topK。 */
    @TableField("top_k")
    private Integer topK;

    /** 引用来源JSON，MVP阶段直接保存响应中的引用列表。 */
    @TableField("citations_json")
    private String citationsJson;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
