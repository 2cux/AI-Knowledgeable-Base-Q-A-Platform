ALTER TABLE `chat_record`
    ADD COLUMN `raw_retrieved_chunk_count` INT NOT NULL DEFAULT 0
        COMMENT 'Raw retrieved chunk count before effective filtering'
        AFTER `retrieved_chunk_count`;
