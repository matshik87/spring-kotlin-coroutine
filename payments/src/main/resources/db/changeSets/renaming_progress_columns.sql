-- liquibase formatted sql
-- changeset maciej.parandyk:"renaming process name into process type"
ALTER TABLE payments.process_progress RENAME COLUMN process_name TO process_type;