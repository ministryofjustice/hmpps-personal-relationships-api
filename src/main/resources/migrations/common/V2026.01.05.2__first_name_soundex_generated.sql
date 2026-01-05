ALTER TABLE contact ADD COLUMN first_name_soundex CHAR(4) GENERATED ALWAYS AS (soundex(first_name)) STORED;

create index concurrently idx_contact_first_name_soundex on contact (first_name_soundex);
