CREATE INDEX `idx_chat_record_matched_created`
    ON `chat_record` (`matched`, `created_at`, `id`);

CREATE INDEX `idx_chat_record_kb_matched_created`
    ON `chat_record` (`knowledge_base_id`, `matched`, `created_at`, `id`);
