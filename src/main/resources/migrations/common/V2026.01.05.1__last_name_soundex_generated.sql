ALTER TABLE contact ADD COLUMN last_name_soundex CHAR(4) GENERATED ALWAYS AS (soundex(last_name)) STORED;

create index concurrently idx_contact_last_name_soundex on contact (last_name_soundex);
