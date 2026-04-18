ALTER TABLE `document`
    ADD INDEX `idx_document_owner_kb_created_at` (`created_by`, `knowledge_base_id`, `created_at`);

ALTER TABLE `task_record`
    ADD INDEX `idx_task_record_status_created_at` (`status`, `created_at`);
