--
-- The current and historical rows for prisoner restrictions
-- authorised_username maps to authorised_staff_id in offender_restriction table in NOMIS
-- created_by maps to entered_staff_id value in offender_restriction table in NOMIS
-- this is the staff member who created the record
--
create table prisoner_restrictions
(
    prisoner_restriction_id bigserial NOT NULL
        CONSTRAINT prisoner_restriction_id_pk PRIMARY KEY,
    prisoner_number         varchar(7) NOT NULL,
    restriction_type        varchar(12),
    effective_date        date,
    expiry_date        date,
    comment_text        varchar(240),
    authorised_username     varchar(100) NOT NULL,
    current_term boolean NOT NULL DEFAULT true, -- True if it applies to latest booking sequence 1
    created_by varchar(100) NOT NULL,
    created_time timestamp NOT NULL DEFAULT current_timestamp,
    updated_by varchar(100),
    updated_time timestamp
);
create index prisoner_restrictions_prisoner_number_idx on prisoner_restrictions (prisoner_number);
create index prisoner_restrictions_restriction_type_idx on prisoner_restrictions (restriction_type);
