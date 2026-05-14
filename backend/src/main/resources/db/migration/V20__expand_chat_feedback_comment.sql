ALTER TABLE `chat_feedback`
    MODIFY COLUMN `comment` VARCHAR(600) DEFAULT NULL COMMENT 'Feedback reason prefix plus optional user comment';
