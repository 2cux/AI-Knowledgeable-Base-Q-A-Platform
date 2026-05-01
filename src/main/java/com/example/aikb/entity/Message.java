package com.example.aikb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * One user or assistant message in a conversation.
 */
@Data
@TableName("message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("message_uid")
    private String messageUid;

    @TableField("conversation_uid")
    private String conversationUid;

    @TableField("user_id")
    private Long userId;

    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;

    private String role;

    private String content;

    private String citations;

    @TableField("chat_record_id")
    private Long chatRecordId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    private Boolean deleted;
}
