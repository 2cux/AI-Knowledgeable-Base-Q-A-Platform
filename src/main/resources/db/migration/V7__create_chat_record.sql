CREATE TABLE IF NOT EXISTS `chat_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '提问用户ID',
    `knowledge_base_id` BIGINT NOT NULL COMMENT '知识库ID',
    `question` TEXT NOT NULL COMMENT '用户问题',
    `answer` MEDIUMTEXT NOT NULL COMMENT '生成答案',
    `matched` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否检索命中切片',
    `retrieved_chunk_count` INT NOT NULL DEFAULT 0 COMMENT '命中的切片数量',
    `top_k` INT NOT NULL DEFAULT 5 COMMENT '请求的topK',
    `citations_json` TEXT DEFAULT NULL COMMENT '答案引用来源JSON',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_chat_record_user_created` (`user_id`, `created_at`),
    KEY `idx_chat_record_kb_created` (`knowledge_base_id`, `created_at`),
    CONSTRAINT `fk_chat_record_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_chat_record_kb_id` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答记录表';
