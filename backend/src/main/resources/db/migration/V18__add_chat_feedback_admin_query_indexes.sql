ALTER TABLE `chat_feedback`
    ADD KEY `idx_chat_feedback_created` (`created_at`),
    ADD KEY `idx_chat_feedback_type_created` (`feedback_type`, `created_at`);
