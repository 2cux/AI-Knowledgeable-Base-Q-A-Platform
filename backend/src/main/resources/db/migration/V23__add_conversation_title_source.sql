ALTER TABLE `conversation`
    ADD COLUMN `title_source` VARCHAR(16) NOT NULL DEFAULT 'AUTO'
        COMMENT 'Title source: AUTO = auto-generated, USER = user-renamed' AFTER `title`;
