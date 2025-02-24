--
-- The current and historical rows for number of children
--
create table prisoner_number_of_children
(
    prisoner_number_of_children_id                   bigserial  NOT NULL
        CONSTRAINT prisoner_number_of_children_id_pk PRIMARY KEY,
    prisoner_number      varchar(7) NOT NULL,
    number_of_children varchar(50),
    active               boolean    NOT NULL,
    created_by           varchar(100),
    created_time         timestamp  NOT NULL DEFAULT current_timestamp
);

create index prisoner_number_of_children_prisoner_number_idx on prisoner_number_of_children (prisoner_number);
create index prisoner_number_of_children_code_idx on prisoner_number_of_children (number_of_children);