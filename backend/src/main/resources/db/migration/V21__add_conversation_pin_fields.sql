DELIMITER //

CREATE PROCEDURE add_column_if_missing(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN ddl_value TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND column_name = column_name_value
    ) THEN
        SET @ddl = ddl_value;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

CREATE PROCEDURE add_index_if_missing(
    IN table_name_value VARCHAR(64),
    IN index_name_value VARCHAR(64),
    IN ddl_value TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND index_name = index_name_value
    ) THEN
        SET @ddl = ddl_value;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CALL add_column_if_missing('conversation', 'pinned',
    'ALTER TABLE `conversation` ADD COLUMN `pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Pinned conversation flag'' AFTER `deleted`');
CALL add_column_if_missing('conversation', 'pinned_at',
    'ALTER TABLE `conversation` ADD COLUMN `pinned_at` DATETIME NULL COMMENT ''Pinned time'' AFTER `pinned`');

CALL add_index_if_missing('conversation', 'idx_conversation_user_kb_pin_active',
    'CREATE INDEX `idx_conversation_user_kb_pin_active` ON `conversation` (`user_id`, `knowledge_base_id`, `pinned`, `pinned_at`, `last_active_at`)');

DROP PROCEDURE add_index_if_missing;
DROP PROCEDURE add_column_if_missing;
