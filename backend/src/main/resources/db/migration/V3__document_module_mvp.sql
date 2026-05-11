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
