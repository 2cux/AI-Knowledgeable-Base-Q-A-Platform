package com.example.aikb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.aikb.service.chat.AnswerStatus;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Chat record entity for one ask request.
 */
@Data
@TableName("chat_record")
public class ChatRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("scope_type")
    private String scopeType;

    @TableField("conversation_id")
    private String conversationId;

    private String question;

    private String answer;

    @TableField("answer_status")
    private AnswerStatus answerStatus;

    /**
     * True only when at least one effective chunk can be used as answer evidence.
     */
    private Boolean matched;

    /**
     * Effective chunk count after retrieval quality filtering.
     */
    @TableField("retrieved_chunk_count")
    private Integer retrievedChunkCount;

    /**
     * Raw vector retrieval count before effective-score filtering.
     */
    @TableField("raw_retrieved_chunk_count")
    private Integer rawRetrievedChunkCount;

    /**
     * Requested retrieval topK for this ask.
     */
    @TableField("top_k")
    private Integer topK;

    /**
     * Serialized answer citations used for detail traceability.
     */
    @TableField("citations_json")
    private String citationsJson;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
