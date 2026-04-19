CREATE TABLE IF NOT EXISTS `chat_feedback` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `chat_record_id` BIGINT NOT NULL COMMENT '问答记录ID',
    `user_id` BIGINT NOT NULL COMMENT '提交反馈的用户ID',
    `feedback_type` VARCHAR(32) NOT NULL COMMENT '反馈类型：LIKE 或 DISLIKE',
    `comment` VARCHAR(500) DEFAULT NULL COMMENT '反馈备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chat_feedback_record_user` (`chat_record_id`, `user_id`),
    KEY `idx_chat_feedback_user_created` (`user_id`, `created_at`),
    CONSTRAINT `fk_chat_feedback_record_id` FOREIGN KEY (`chat_record_id`) REFERENCES `chat_record` (`id`),
    CONSTRAINT `fk_chat_feedback_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答反馈表';
