ALTER TABLE contact ADD COLUMN middle_names_soundex CHAR(4) GENERATED ALWAYS AS (soundex(middle_names)) STORED;

create index idx_contact_middle_names_soundex on contact (middle_names_soundex);
