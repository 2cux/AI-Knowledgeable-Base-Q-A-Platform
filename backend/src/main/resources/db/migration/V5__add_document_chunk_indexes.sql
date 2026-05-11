ALTER TABLE `document_chunk`
    ADD INDEX `idx_chunk_document_index` (`document_id`, `chunk_index`);
