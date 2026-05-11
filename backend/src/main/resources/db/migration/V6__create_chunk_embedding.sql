CREATE TABLE IF NOT EXISTS `chunk_embedding` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `document_id` BIGINT NOT NULL COMMENT 'Document id',
    `knowledge_base_id` BIGINT NOT NULL COMMENT 'Knowledge base id',
    `chunk_id` BIGINT NOT NULL COMMENT 'Document chunk id',
    `embedding_model` VARCHAR(128) NOT NULL COMMENT 'Embedding model name',
    `vector_id` VARCHAR(255) DEFAULT NULL COMMENT 'Vector id in vector store',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'Embedding status: PENDING, SUCCESS, FAILED',
    `embedding_error` VARCHAR(1000) DEFAULT NULL COMMENT 'Embedding error message',
    `embedded_at` DATETIME DEFAULT NULL COMMENT 'Embedded time',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chunk_embedding_chunk_id` (`chunk_id`),
    KEY `idx_chunk_embedding_document_id` (`document_id`),
    KEY `idx_chunk_embedding_kb_status` (`knowledge_base_id`, `status`),
    CONSTRAINT `fk_chunk_embedding_document_id` FOREIGN KEY (`document_id`) REFERENCES `document` (`id`),
    CONSTRAINT `fk_chunk_embedding_chunk_id` FOREIGN KEY (`chunk_id`) REFERENCES `document_chunk` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Chunk embedding status table';
