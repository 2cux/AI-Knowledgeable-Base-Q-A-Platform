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

CREATE PROCEDURE modify_column_if_exists(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN ddl_value TEXT
)
BEGIN
    IF EXISTS (
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

CALL add_column_if_missing('conversation', 'scope_type',
    'ALTER TABLE `conversation` ADD COLUMN `scope_type` VARCHAR(32) NOT NULL DEFAULT ''KNOWLEDGE_BASE'' COMMENT ''Conversation search scope'' AFTER `knowledge_base_id`');
CALL add_column_if_missing('message', 'scope_type',
    'ALTER TABLE `message` ADD COLUMN `scope_type` VARCHAR(32) NOT NULL DEFAULT ''KNOWLEDGE_BASE'' COMMENT ''Message search scope'' AFTER `knowledge_base_id`');
CALL add_column_if_missing('chat_record', 'scope_type',
    'ALTER TABLE `chat_record` ADD COLUMN `scope_type` VARCHAR(32) NOT NULL DEFAULT ''KNOWLEDGE_BASE'' COMMENT ''Ask search scope'' AFTER `knowledge_base_id`');

CALL modify_column_if_exists('conversation', 'knowledge_base_id',
    'ALTER TABLE `conversation` MODIFY COLUMN `knowledge_base_id` BIGINT NULL COMMENT ''Knowledge base ID; NULL means enterprise-wide chat''');
CALL modify_column_if_exists('message', 'knowledge_base_id',
    'ALTER TABLE `message` MODIFY COLUMN `knowledge_base_id` BIGINT NULL COMMENT ''Knowledge base ID; NULL means enterprise-wide chat''');
CALL modify_column_if_exists('chat_record', 'knowledge_base_id',
    'ALTER TABLE `chat_record` MODIFY COLUMN `knowledge_base_id` BIGINT NULL COMMENT ''Knowledge base ID; NULL means enterprise-wide chat''');

CALL add_index_if_missing('conversation', 'idx_conversation_user_scope_active',
    'CREATE INDEX `idx_conversation_user_scope_active` ON `conversation` (`user_id`, `scope_type`, `last_active_at`)');
CALL add_index_if_missing('message', 'idx_message_user_scope_conversation',
    'CREATE INDEX `idx_message_user_scope_conversation` ON `message` (`user_id`, `scope_type`, `conversation_uid`)');
CALL add_index_if_missing('chat_record', 'idx_chat_record_scope_created',
    'CREATE INDEX `idx_chat_record_scope_created` ON `chat_record` (`scope_type`, `created_at`, `id`)');

DROP PROCEDURE add_index_if_missing;
DROP PROCEDURE modify_column_if_exists;
DROP PROCEDURE add_column_if_missing;
