ALTER TABLE `document`
    ADD COLUMN `embedding_status` VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED' COMMENT 'Document embedding status' AFTER `parse_status`,
    ADD COLUMN `chunk_count` INT NOT NULL DEFAULT 0 COMMENT 'Current chunk count' AFTER `embedding_status`,
    ADD COLUMN `embedded_chunk_count` INT NOT NULL DEFAULT 0 COMMENT 'Successfully embedded chunk count' AFTER `chunk_count`,
    ADD COLUMN `latest_task_type` VARCHAR(32) DEFAULT NULL COMMENT 'Latest lifecycle task type' AFTER `embedded_chunk_count`,
    ADD COLUMN `latest_task_status` VARCHAR(32) DEFAULT NULL COMMENT 'Latest lifecycle task status' AFTER `latest_task_type`,
    ADD COLUMN `latest_error_message` VARCHAR(1000) DEFAULT NULL COMMENT 'Latest lifecycle error message' AFTER `latest_task_status`;

UPDATE `document`
SET `parse_status` = CASE
        WHEN `parse_status` IN ('CHUNKED', 'DONE') THEN 'SUCCESS'
        WHEN `parse_status` IN ('CHUNKING', 'PROCESSING') THEN 'PROCESSING'
        WHEN `parse_status` = 'FAILED' THEN 'FAILED'
        ELSE 'NOT_STARTED'
    END;

UPDATE `document` d
SET d.`chunk_count` = (
        SELECT COUNT(1)
        FROM `document_chunk` dc
        WHERE dc.`document_id` = d.`id`
    ),
    d.`embedded_chunk_count` = (
        SELECT COUNT(1)
        FROM `chunk_embedding` ce
        WHERE ce.`document_id` = d.`id`
          AND ce.`status` = 'SUCCESS'
    );

UPDATE `document`
SET `embedding_status` = CASE
        WHEN `chunk_count` = 0 THEN 'NOT_STARTED'
        WHEN `embedded_chunk_count` = `chunk_count` THEN 'SUCCESS'
        WHEN `embedded_chunk_count` > 0 THEN 'PARTIAL_SUCCESS'
        ELSE 'NOT_STARTED'
    END;

ALTER TABLE `document`
    ADD INDEX `idx_document_lifecycle_status` (`parse_status`, `embedding_status`);
