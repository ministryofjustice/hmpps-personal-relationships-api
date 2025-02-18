--
-- The current and historical rows for domestic status
--
create table prisoner_domestic_status
(
    id                   bigserial  NOT NULL
        CONSTRAINT prisoner_domestic_status_id_pk PRIMARY KEY,
    prisoner_number      varchar(7) NOT NULL,
    domestic_status_code char(1),
    active               boolean    NOT NULL,
    created_by           varchar(100),
    created_time         timestamp  NOT NULL DEFAULT current_timestamp
);

create index prisoner_domestic_status_prisoner_number_idx on prisoner_domestic_status (prisoner_number);
create index prisoner_domestic_status_code_idx on prisoner_domestic_status (domestic_status_code);
create index prisoner_domestic_status_active_idx on prisoner_domestic_status (active);