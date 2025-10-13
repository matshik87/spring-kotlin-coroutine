-- liquibase formatted sql
-- changeset maciej.parandyk:"adding residency country code to customer table"
ALTER TABLE payments.customer
    ADD COLUMN residency_country_code VARCHAR(2);

-- liquibase formatted sql
-- changeset maciej.parandyk:"populating missing residency country code using nationality"
UPDATE payments.customer
SET residency_country_code = nationality;

-- liquibase formatted sql
-- changeset maciej.parandyk:"setting residency country code not nullable"
ALTER TABLE payments.customer
    ALTER COLUMN residency_country_code SET NOT NULL;