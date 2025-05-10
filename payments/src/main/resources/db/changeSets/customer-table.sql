-- liquibase formatted sql
-- changeset maciej.parandyk:"creating customer table"
CREATE TABLE payments.customer
(
    id                     uuid,
    first_name             varchar(40)  not null,
    middle_name            varchar(40),
    last_name              varchar(60)  not null,
    dob                    date         not null,
    nationality            varchar(2)   not null,
    login                  varchar(10)  not null,
    password               varchar(45)  not null,
    email                  varchar(100) not null,
    phone_number           varchar(20)  not null,
    secondary_phone_number varchar(20)  not null,
    CONSTRAINT pk_customer_id PRIMARY KEY (id),
    CONSTRAINT uq_customer_login UNIQUE (login),
    CONSTRAINT uq_customer_email UNIQUE (email),
    CONSTRAINT uq_customer_phone_number UNIQUE (phone_number)
);