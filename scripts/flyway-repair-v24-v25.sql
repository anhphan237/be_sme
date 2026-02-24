-- Fix Flyway checksum mismatch for V24, V25
-- Run this when migration files were modified and DB was updated accordingly.
-- Uses checksums from error "Resolved locally".
-- Table: flyway_schema_history (default) or spring.flyway.table if customized

UPDATE flyway_schema_history SET checksum = -1932779435 WHERE version = '24';
UPDATE flyway_schema_history SET checksum = 353615513   WHERE version = '25';
