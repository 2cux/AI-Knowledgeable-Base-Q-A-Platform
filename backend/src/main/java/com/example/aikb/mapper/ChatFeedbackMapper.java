package com.example.aikb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aikb.dto.chat.AdminChatFeedbackQueryRow;
import com.example.aikb.dto.chat.AdminFeedbackStatsRow;
import com.example.aikb.entity.ChatFeedback;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 问答反馈数据访问接口，提供 chat_feedback 表的基础 CRUD 能力。
 */
public interface ChatFeedbackMapper extends BaseMapper<ChatFeedback> {

    @Select("""
            <script>
            SELECT
                f.id,
                f.chat_record_id AS chatRecordId,
                f.user_id AS userId,
                r.knowledge_base_id AS knowledgeBaseId,
                r.conversation_id AS conversationId,
                r.question,
                r.answer,
                f.feedback_type AS feedbackType,
                f.comment,
                f.created_at AS createdAt
            FROM chat_feedback f
            INNER JOIN chat_record r ON r.id = f.chat_record_id
            WHERE 1 = 1
            <if test="knowledgeBaseId != null">
                AND r.knowledge_base_id = #{knowledgeBaseId}
            </if>
            <if test="feedbackType != null and feedbackType != ''">
                AND f.feedback_type = #{feedbackType}
            </if>
            <if test="reasonPattern != null and reasonPattern != ''">
                AND f.comment LIKE #{reasonPattern}
            </if>
            <if test="startTime != null">
                AND f.created_at &gt;= #{startTime}
            </if>
            <if test="endTime != null">
                AND f.created_at &lt;= #{endTime}
            </if>
            ORDER BY f.created_at DESC, f.id DESC
            </script>
            """)
    IPage<AdminChatFeedbackQueryRow> selectAdminFeedbackPage(Page<AdminChatFeedbackQueryRow> page,
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("feedbackType") String feedbackType,
            @Param("reasonPattern") String reasonPattern,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Select("""
            <script>
            SELECT
                COUNT(*) AS feedbackCount,
                COALESCE(SUM(CASE WHEN f.feedback_type = 'LIKE' THEN 1 ELSE 0 END), 0) AS likeCount,
                COALESCE(SUM(CASE WHEN f.feedback_type = 'DISLIKE' THEN 1 ELSE 0 END), 0) AS dislikeCount
            FROM chat_feedback f
            INNER JOIN chat_record r ON r.id = f.chat_record_id
            WHERE 1 = 1
            <if test="knowledgeBaseId != null">
                AND r.knowledge_base_id = #{knowledgeBaseId}
            </if>
            <if test="startTime != null">
                AND f.created_at &gt;= #{startTime}
            </if>
            <if test="endTime != null">
                AND f.created_at &lt;= #{endTime}
            </if>
            </script>
            """)
    AdminFeedbackStatsRow selectAdminFeedbackStats(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
