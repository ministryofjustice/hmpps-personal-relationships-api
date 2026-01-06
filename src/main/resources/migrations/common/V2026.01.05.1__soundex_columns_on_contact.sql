--
-- In real environments - run all of these SQL statements manually, ahead of time, as they take too long for Flyway migrations.
-- In tests or running against local DBs with small data sets, they are fine.
--
ALTER TABLE contact ADD COLUMN IF NOT EXISTS last_name_soundex CHAR(4) GENERATED ALWAYS AS (soundex(last_name)) STORED;
ALTER TABLE contact ADD COLUMN IF NOT EXISTS first_name_soundex CHAR(4) GENERATED ALWAYS AS (soundex(first_name)) STORED;
ALTER TABLE contact ADD COLUMN IF NOT EXISTS middle_names_soundex CHAR(4) GENERATED ALWAYS AS (soundex(middle_names)) STORED;

-- Index the soundex columns on contact
create index IF NOT EXISTS idx_contact_last_name_soundex on contact (last_name_soundex);
create index IF NOT EXISTS idx_contact_first_name_soundex on contact (first_name_soundex);
create index IF NOT EXISTS idx_contact_middle_names_soundex on contact (middle_names_soundex);

-- End