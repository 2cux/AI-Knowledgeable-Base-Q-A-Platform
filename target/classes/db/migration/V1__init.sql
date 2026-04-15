CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` VARCHAR(64) NOT NULL COMMENT '登录用户名',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
    `nickname` VARCHAR(64) NOT NULL COMMENT '用户昵称',
    `role` VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '角色',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态，1启用，0禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(128) NOT NULL COMMENT '知识库名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '知识库描述',
    `owner_id` BIGINT NOT NULL COMMENT '创建人ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态，1正常，0禁用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_kb_owner_id` (`owner_id`),
    CONSTRAINT `fk_kb_owner_id` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';

CREATE TABLE IF NOT EXISTS `document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `knowledge_base_id` BIGINT NOT NULL COMMENT '所属知识库ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_type` VARCHAR(32) NOT NULL COMMENT '文件类型',
    `file_size` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小，字节',
    `storage_path` VARCHAR(500) NOT NULL COMMENT '存储路径',
    `parse_status` VARCHAR(32) NOT NULL DEFAULT 'UPLOADED' COMMENT '解析状态',
    `created_by` BIGINT NOT NULL COMMENT '上传人ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_document_kb_id` (`knowledge_base_id`),
    KEY `idx_document_created_by` (`created_by`),
    CONSTRAINT `fk_document_kb_id` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`),
    CONSTRAINT `fk_document_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

CREATE TABLE IF NOT EXISTS `document_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `document_id` BIGINT NOT NULL COMMENT '所属文档ID',
    `knowledge_base_id` BIGINT NOT NULL COMMENT '所属知识库ID',
    `chunk_index` INT NOT NULL COMMENT '片段序号',
    `content` TEXT NOT NULL COMMENT '切片内容',
    `token_count` INT DEFAULT NULL COMMENT 'token数量',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_chunk_document_id` (`document_id`),
    KEY `idx_chunk_kb_id` (`knowledge_base_id`),
    CONSTRAINT `fk_chunk_document_id` FOREIGN KEY (`document_id`) REFERENCES `document` (`id`),
    CONSTRAINT `fk_chunk_kb_id` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档切片表';

CREATE TABLE IF NOT EXISTS `conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `knowledge_base_id` BIGINT NOT NULL COMMENT '知识库ID',
    `title` VARCHAR(255) NOT NULL COMMENT '会话标题',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_user_id` (`user_id`),
    KEY `idx_conversation_kb_id` (`knowledge_base_id`),
    CONSTRAINT `fk_conversation_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_conversation_kb_id` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
    `role` VARCHAR(32) NOT NULL COMMENT '消息角色，USER或ASSISTANT',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `message_type` VARCHAR(32) NOT NULL COMMENT '消息类型，QUESTION或ANSWER',
    `quote_json` TEXT DEFAULT NULL COMMENT '引用片段JSON',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_message_conversation_id` (`conversation_id`),
    CONSTRAINT `fk_message_conversation_id` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

CREATE TABLE IF NOT EXISTS `task_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_type` VARCHAR(32) NOT NULL COMMENT '任务类型',
    `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
    `biz_id` BIGINT NOT NULL COMMENT '业务ID',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
    `error_message` VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_record_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务记录表';
