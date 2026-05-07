ALTER TABLE `knowledge_base`
    ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Soft delete flag: 0 active, 1 deleted' AFTER `status`;

CREATE INDEX `idx_kb_owner_deleted` ON `knowledge_base` (`owner_id`, `deleted`);
