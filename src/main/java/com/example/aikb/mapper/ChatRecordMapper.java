package com.example.aikb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aikb.dto.chat.AdminChatStatsCountRow;
import com.example.aikb.entity.ChatRecord;
import com.example.aikb.vo.chat.AdminHotQuestionVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 问答记录数据访问接口，提供 chat_record 表的基础 CRUD 能力。
 */
public interface ChatRecordMapper extends BaseMapper<ChatRecord> {

    @Select("""
            <script>
            SELECT
                question,
                COUNT(*) AS count,
                MAX(created_at) AS latestAskedAt
            FROM chat_record
            WHERE question IS NOT NULL
              AND TRIM(question) != ''
            <if test="knowledgeBaseId != null">
                AND knowledge_base_id = #{knowledgeBaseId}
            </if>
            <if test="startTime != null">
                AND created_at &gt;= #{startTime}
            </if>
            <if test="endTime != null">
                AND created_at &lt;= #{endTime}
            </if>
            GROUP BY question
            ORDER BY count DESC, latestAskedAt DESC
            LIMIT #{limit}
            </script>
            """)
    List<AdminHotQuestionVO> selectHotQuestions(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") int limit);

    @Select("""
            <script>
            SELECT
                COUNT(*) AS totalChatCount,
                COALESCE(SUM(CASE WHEN matched = 1 THEN 1 ELSE 0 END), 0) AS matchedCount,
                COALESCE(SUM(CASE WHEN matched = 0 THEN 1 ELSE 0 END), 0) AS missedCount
            FROM chat_record
            WHERE 1 = 1
            <if test="knowledgeBaseId != null">
                AND knowledge_base_id = #{knowledgeBaseId}
            </if>
            <if test="startTime != null">
                AND created_at &gt;= #{startTime}
            </if>
            <if test="endTime != null">
                AND created_at &lt;= #{endTime}
            </if>
            </script>
            """)
    AdminChatStatsCountRow selectAdminChatStats(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Select("""
            <script>
            SELECT
                COUNT(*) AS totalChatCount,
                COALESCE(SUM(CASE WHEN matched = 1 THEN 1 ELSE 0 END), 0) AS matchedCount,
                COALESCE(SUM(CASE WHEN matched = 0 THEN 1 ELSE 0 END), 0) AS missedCount
            FROM chat_record
            WHERE created_at &gt;= #{todayStart}
              AND created_at &lt; #{tomorrowStart}
            <if test="knowledgeBaseId != null">
                AND knowledge_base_id = #{knowledgeBaseId}
            </if>
            </script>
            """)
    AdminChatStatsCountRow selectAdminTodayChatStats(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("todayStart") LocalDateTime todayStart,
            @Param("tomorrowStart") LocalDateTime tomorrowStart);
}
