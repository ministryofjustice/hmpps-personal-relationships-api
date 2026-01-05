ALTER TABLE contact ADD COLUMN first_name_soundex CHAR(4) GENERATED ALWAYS AS (soundex(first_name)) STORED;
