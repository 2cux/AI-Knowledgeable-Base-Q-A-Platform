package com.example.aikb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 问答反馈实体，记录用户对某次问答结果的点赞或点踩。
 */
@Data
@TableName("chat_feedback")
public class ChatFeedback {

    /** 反馈记录主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被反馈的问答记录ID。 */
    @TableField("chat_record_id")
    private Long chatRecordId;

    /** 提交反馈的用户ID。 */
    @TableField("user_id")
    private Long userId;

    /** 反馈类型：LIKE 或 DISLIKE。 */
    @TableField("feedback_type")
    private String feedbackType;

    /** 用户补充说明。 */
    private String comment;

    /** 创建时间。 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
