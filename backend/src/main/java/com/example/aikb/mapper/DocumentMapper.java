package com.example.aikb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aikb.entity.Document;
import org.apache.ibatis.annotations.Select;

/**
 * 文档数据访问接口，提供文档表基础 CRUD 能力。
 */
public interface DocumentMapper extends BaseMapper<Document> {

    @Select("""
            SELECT COUNT(*)
            FROM document d
            INNER JOIN knowledge_base kb ON kb.id = d.knowledge_base_id
            WHERE kb.deleted IS NULL OR kb.deleted != 1
            """)
    Long countAdminVisibleDocuments();
}
