-- liquibase formatted sql
-- changeset maciej.parandyk:"adding process parent id to table"
ALTER TABLE payments.process_progress
    ADD COLUMN process_parent_id uuid;