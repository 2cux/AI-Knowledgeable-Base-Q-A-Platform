ALTER TABLE `chat_record`
    ADD COLUMN `answer_status` VARCHAR(32) NULL COMMENT 'RAG answer status' AFTER `answer`;

UPDATE `chat_record`
SET `answer_status` = CASE
    WHEN `matched` = 1 THEN 'SUCCESS'
    ELSE 'NO_HIT'
END
WHERE `answer_status` IS NULL;

ALTER TABLE `chat_record`
    MODIFY COLUMN `answer_status` VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT 'RAG answer status';

CREATE INDEX `idx_chat_record_answer_status_created`
    ON `chat_record` (`answer_status`, `created_at`, `id`);
