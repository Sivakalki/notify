ALTER TABLE uploaded_files
    ADD COLUMN duplicate_count INT NOT NULL DEFAULT 0;
