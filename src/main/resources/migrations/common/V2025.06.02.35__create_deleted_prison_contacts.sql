-- Table to keep a history of deleted prisoner contacts

CREATE TABLE deleted_prisoner_contact
(
    deleted_prisoner_contact_id bigserial      NOT NULL CONSTRAINT deleted_prisoner_contact_id_pk PRIMARY KEY,
    prisoner_contact_id         bigint         NOT NULL,
    contact_id                  bigint         NOT NULL,
    prisoner_number             varchar(7)     NOT NULL,
    active                      boolean,
    relationship_type           varchar(12),
    relationship_to_prisoner    varchar(12),
    current_term                boolean,
    approved_visitor            boolean,
    next_of_kin                 boolean,
    emergency_contact           boolean,
    comments                    varchar(240),
    approved_by                 varchar(100),
    approved_time               timestamp,
    expiry_date                 date,
    created_at_prison           varchar(5),
    created_by                  varchar(100),
    created_time                timestamp,
    updated_by                  varchar(100),
    updated_time                timestamp,
    deleted_by                  varchar(100)   NOT NULL,
    deleted_time                timestamp      NOT NULL
);
