ALTER TABLE contact_audit ADD COLUMN last_name_soundex CHAR(4) GENERATED ALWAYS AS (soundex(last_name)) STORED;
