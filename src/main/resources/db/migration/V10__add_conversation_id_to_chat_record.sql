ALTER TABLE `chat_record`
    ADD COLUMN `conversation_id` VARCHAR(64) DEFAULT NULL COMMENT '会话ID' AFTER `knowledge_base_id`;

CREATE INDEX `idx_chat_record_conversation_created`
    ON `chat_record` (`user_id`, `knowledge_base_id`, `conversation_id`, `created_at`, `id`);
