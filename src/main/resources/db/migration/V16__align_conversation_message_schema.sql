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

DELIMITER ;

CALL add_column_if_missing('conversation', 'conversation_uid',
    'ALTER TABLE `conversation` ADD COLUMN `conversation_uid` VARCHAR(64) NULL COMMENT ''Business conversation ID'' AFTER `id`');
CALL add_column_if_missing('conversation', 'message_count',
    'ALTER TABLE `conversation` ADD COLUMN `message_count` INT NOT NULL DEFAULT 0 COMMENT ''Message count'' AFTER `title`');
CALL add_column_if_missing('conversation', 'last_question',
    'ALTER TABLE `conversation` ADD COLUMN `last_question` TEXT DEFAULT NULL COMMENT ''Last user question'' AFTER `message_count`');
CALL add_column_if_missing('conversation', 'last_answer_preview',
    'ALTER TABLE `conversation` ADD COLUMN `last_answer_preview` VARCHAR(300) DEFAULT NULL COMMENT ''Last assistant answer preview'' AFTER `last_question`');
CALL add_column_if_missing('conversation', 'last_active_at',
    'ALTER TABLE `conversation` ADD COLUMN `last_active_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''Last active time'' AFTER `last_answer_preview`');
CALL add_column_if_missing('conversation', 'deleted',
    'ALTER TABLE `conversation` ADD COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Logical delete flag'' AFTER `updated_at`');

UPDATE `conversation`
SET `conversation_uid` = UUID()
WHERE `conversation_uid` IS NULL OR `conversation_uid` = '';

ALTER TABLE `conversation`
    MODIFY COLUMN `conversation_uid` VARCHAR(64) NOT NULL COMMENT 'Business conversation ID';

UPDATE `conversation`
SET `last_active_at` = COALESCE(`last_active_at`, `updated_at`, `created_at`, CURRENT_TIMESTAMP);

CALL add_index_if_missing('conversation', 'uk_conversation_uid',
    'CREATE UNIQUE INDEX `uk_conversation_uid` ON `conversation` (`conversation_uid`)');
CALL add_index_if_missing('conversation', 'idx_conversation_user_kb_active',
    'CREATE INDEX `idx_conversation_user_kb_active` ON `conversation` (`user_id`, `knowledge_base_id`, `last_active_at`)');

CALL add_column_if_missing('message', 'message_uid',
    'ALTER TABLE `message` ADD COLUMN `message_uid` VARCHAR(64) NULL COMMENT ''Business message ID'' AFTER `id`');
CALL add_column_if_missing('message', 'conversation_uid',
    'ALTER TABLE `message` ADD COLUMN `conversation_uid` VARCHAR(64) NULL COMMENT ''Business conversation ID'' AFTER `message_uid`');
CALL add_column_if_missing('message', 'user_id',
    'ALTER TABLE `message` ADD COLUMN `user_id` BIGINT NULL COMMENT ''User ID'' AFTER `conversation_uid`');
CALL add_column_if_missing('message', 'knowledge_base_id',
    'ALTER TABLE `message` ADD COLUMN `knowledge_base_id` BIGINT NULL COMMENT ''Knowledge base ID'' AFTER `user_id`');
CALL add_column_if_missing('message', 'citations',
    'ALTER TABLE `message` ADD COLUMN `citations` TEXT DEFAULT NULL COMMENT ''Answer citations JSON'' AFTER `content`');
CALL add_column_if_missing('message', 'chat_record_id',
    'ALTER TABLE `message` ADD COLUMN `chat_record_id` BIGINT DEFAULT NULL COMMENT ''Related chat_record.id'' AFTER `citations`');
CALL add_column_if_missing('message', 'deleted',
    'ALTER TABLE `message` ADD COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Logical delete flag'' AFTER `created_at`');

UPDATE `message`
SET `message_uid` = UUID()
WHERE `message_uid` IS NULL OR `message_uid` = '';

UPDATE `message` m
JOIN `conversation` c ON m.`conversation_id` = c.`id`
SET m.`conversation_uid` = c.`conversation_uid`,
    m.`user_id` = c.`user_id`,
    m.`knowledge_base_id` = c.`knowledge_base_id`
WHERE m.`conversation_uid` IS NULL
   OR m.`user_id` IS NULL
   OR m.`knowledge_base_id` IS NULL;

UPDATE `message`
SET `citations` = COALESCE(`citations`, `quote_json`, '[]')
WHERE `citations` IS NULL;

CALL modify_column_if_exists('message', 'conversation_id',
    'ALTER TABLE `message` MODIFY COLUMN `conversation_id` BIGINT NULL COMMENT ''Legacy numeric conversation id''');
CALL modify_column_if_exists('message', 'message_type',
    'ALTER TABLE `message` MODIFY COLUMN `message_type` VARCHAR(32) NULL COMMENT ''Legacy message type''');
CALL modify_column_if_exists('message', 'content',
    'ALTER TABLE `message` MODIFY COLUMN `content` MEDIUMTEXT NOT NULL COMMENT ''Message content''');

ALTER TABLE `message`
    MODIFY COLUMN `message_uid` VARCHAR(64) NOT NULL COMMENT 'Business message ID',
    MODIFY COLUMN `conversation_uid` VARCHAR(64) NOT NULL COMMENT 'Business conversation ID',
    MODIFY COLUMN `user_id` BIGINT NOT NULL COMMENT 'User ID',
    MODIFY COLUMN `knowledge_base_id` BIGINT NOT NULL COMMENT 'Knowledge base ID';

CALL add_index_if_missing('message', 'uk_message_uid',
    'CREATE UNIQUE INDEX `uk_message_uid` ON `message` (`message_uid`)');
CALL add_index_if_missing('message', 'idx_message_conversation_created',
    'CREATE INDEX `idx_message_conversation_created` ON `message` (`conversation_uid`, `created_at`)');
CALL add_index_if_missing('message', 'idx_message_chat_record_id',
    'CREATE INDEX `idx_message_chat_record_id` ON `message` (`chat_record_id`)');
CALL add_index_if_missing('message', 'idx_message_user_kb_conversation',
    'CREATE INDEX `idx_message_user_kb_conversation` ON `message` (`user_id`, `knowledge_base_id`, `conversation_uid`)');

DROP PROCEDURE modify_column_if_exists;
DROP PROCEDURE add_index_if_missing;
DROP PROCEDURE add_column_if_missing;
