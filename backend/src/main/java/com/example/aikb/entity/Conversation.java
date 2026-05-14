package com.example.aikb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Conversation aggregate for user-side multi-turn chat.
 */
@Data
@TableName("conversation")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("conversation_uid")
    private String conversationUid;

    @TableField("user_id")
    private Long userId;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    @TableField("scope_type")
    private String scopeType;

    private String title;

    @TableField("title_source")
    private String titleSource;

    @TableField("message_count")
    private Integer messageCount;

    @TableField("last_question")
    private String lastQuestion;

    @TableField("last_answer_preview")
    private String lastAnswerPreview;

    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    private Boolean deleted;

    private Boolean pinned;

    @TableField("pinned_at")
    private LocalDateTime pinnedAt;
}
