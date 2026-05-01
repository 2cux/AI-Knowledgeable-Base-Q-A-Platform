ALTER TABLE `chat_record`
    ADD COLUMN `raw_retrieved_chunk_count` INT NOT NULL DEFAULT 0
        COMMENT 'Raw retrieved chunk count before effective filtering'
        AFTER `retrieved_chunk_count`;

UPDATE `chat_record`
SET `raw_retrieved_chunk_count` = `retrieved_chunk_count`
WHERE `retrieved_chunk_count` > 0
  AND `raw_retrieved_chunk_count` = 0;
