-- liquibase formatted sql
-- changeset maciej.parandyk:"adding account table"
CREATE TABLE payments.account
(
    id                 UUID,
    currency_code      varchar(2)  not null,
    balance            numeric(12, 2) default 0,
    name               varchar(50),
    status             varchar(15) not null,
    creation_date      timestamp   not null,
    modification_date  timestamp,
    customer_reference uuid        not null,
    CONSTRAINT pk_account_id PRIMARY KEY (id),
    CONSTRAINT fk_customer_account FOREIGN KEY (customer_reference) REFERENCES payments.customer(id)
);