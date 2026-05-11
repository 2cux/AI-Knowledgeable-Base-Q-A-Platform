ALTER TABLE `task_record`
    ADD COLUMN `created_by` BIGINT DEFAULT NULL COMMENT 'Task creator user id' AFTER `retry_count`,
    ADD COLUMN `started_at` DATETIME DEFAULT NULL COMMENT 'Task start time' AFTER `created_by`,
    ADD COLUMN `finished_at` DATETIME DEFAULT NULL COMMENT 'Task finish time' AFTER `started_at`;

ALTER TABLE `task_record`
    ADD INDEX `idx_task_record_created_by` (`created_by`),
    ADD INDEX `idx_task_record_type_biz_created_at` (`task_type`, `biz_type`, `biz_id`, `created_at`);
