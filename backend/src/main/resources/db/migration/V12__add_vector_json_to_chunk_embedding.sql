ALTER TABLE `chunk_embedding`
    ADD COLUMN `vector_json` LONGTEXT DEFAULT NULL COMMENT 'Embedding vector json' AFTER `vector_id`;
