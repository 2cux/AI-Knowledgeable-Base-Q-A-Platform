ALTER TABLE `user`
    ADD COLUMN `email` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'User email' AFTER `password_hash`;
