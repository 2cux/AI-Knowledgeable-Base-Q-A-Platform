CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `username` VARCHAR(64) NOT NULL COMMENT 'Login username',
    `password_hash` VARCHAR(255) NOT NULL COMMENT 'Password hash',
    `nickname` VARCHAR(64) NOT NULL COMMENT 'Display name',
    `role` VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT 'User role',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 1 enabled, 0 disabled',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User table';

CREATE TABLE IF NOT EXISTS `knowledge_base` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `name` VARCHAR(128) NOT NULL COMMENT 'Knowledge base name',
    `description` VARCHAR(500) DEFAULT NULL COMMENT 'Knowledge base description',
    `owner_id` BIGINT NOT NULL COMMENT 'Owner user id',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 1 active, 0 disabled',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    KEY `idx_kb_owner_id` (`owner_id`),
    CONSTRAINT `fk_kb_owner_id` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Knowledge base table';

CREATE TABLE IF NOT EXISTS `document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `knowledge_base_id` BIGINT NOT NULL COMMENT 'Knowledge base id',
    `file_name` VARCHAR(255) NOT NULL COMMENT 'Original file name',
    `file_type` VARCHAR(32) NOT NULL COMMENT 'File type',
    `file_size` BIGINT NOT NULL DEFAULT 0 COMMENT 'File size in bytes',
    `storage_path` VARCHAR(500) NOT NULL COMMENT 'Storage path',
    `parse_status` VARCHAR(32) NOT NULL DEFAULT 'UPLOADED' COMMENT 'Parse status',
    `created_by` BIGINT NOT NULL COMMENT 'Uploader user id',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    KEY `idx_document_kb_id` (`knowledge_base_id`),
    KEY `idx_document_created_by` (`created_by`),
    CONSTRAINT `fk_document_kb_id` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`),
    CONSTRAINT `fk_document_created_by` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Document table';

CREATE TABLE IF NOT EXISTS `document_chunk` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `document_id` BIGINT NOT NULL COMMENT 'Document id',
    `knowledge_base_id` BIGINT NOT NULL COMMENT 'Knowledge base id',
    `chunk_index` INT NOT NULL COMMENT 'Chunk index',
    `content` TEXT NOT NULL COMMENT 'Chunk content',
    `token_count` INT DEFAULT NULL COMMENT 'Token count',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    PRIMARY KEY (`id`),
    KEY `idx_chunk_document_id` (`document_id`),
    KEY `idx_chunk_kb_id` (`knowledge_base_id`),
    CONSTRAINT `fk_chunk_document_id` FOREIGN KEY (`document_id`) REFERENCES `document` (`id`),
    CONSTRAINT `fk_chunk_kb_id` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Document chunk table';

CREATE TABLE IF NOT EXISTS `conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `user_id` BIGINT NOT NULL COMMENT 'User id',
    `knowledge_base_id` BIGINT NOT NULL COMMENT 'Knowledge base id',
    `title` VARCHAR(255) NOT NULL COMMENT 'Conversation title',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_user_id` (`user_id`),
    KEY `idx_conversation_kb_id` (`knowledge_base_id`),
    CONSTRAINT `fk_conversation_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_conversation_kb_id` FOREIGN KEY (`knowledge_base_id`) REFERENCES `knowledge_base` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Conversation table';

CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `conversation_id` BIGINT NOT NULL COMMENT 'Conversation id',
    `role` VARCHAR(32) NOT NULL COMMENT 'Message role: USER or ASSISTANT',
    `content` TEXT NOT NULL COMMENT 'Message content',
    `message_type` VARCHAR(32) NOT NULL COMMENT 'Message type: QUESTION or ANSWER',
    `quote_json` TEXT DEFAULT NULL COMMENT 'Quoted chunk JSON',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    PRIMARY KEY (`id`),
    KEY `idx_message_conversation_id` (`conversation_id`),
    CONSTRAINT `fk_message_conversation_id` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Message table';

CREATE TABLE IF NOT EXISTS `task_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `task_type` VARCHAR(32) NOT NULL COMMENT 'Task type',
    `biz_type` VARCHAR(32) NOT NULL COMMENT 'Business type',
    `biz_id` BIGINT NOT NULL COMMENT 'Business id',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'Task status',
    `error_message` VARCHAR(1000) DEFAULT NULL COMMENT 'Error message',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT 'Retry count',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    KEY `idx_task_record_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Task record table';
