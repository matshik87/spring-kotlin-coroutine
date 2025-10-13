-- liquibase formatted sql
-- changeset maciej.parandyk:"creating process_progress table for tracing processes progress"
CREATE TABLE payments.process_progress
(
    process_id             uuid not null,
    process_name           varchar(25) not null,
    progress               varchar(25) not null,
    entity_id              uuid,
    details                text,
    creation_date          timestamp not null default now(),
    modification_date      timestamp not null,
    CONSTRAINT pk_process_progress_key PRIMARY KEY (process_id, process_name)
);