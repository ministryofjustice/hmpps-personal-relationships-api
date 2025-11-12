
-- Add soundex generated columns for better performance on soundex searches
-- This is the test-only version of the migration script
-- This includes the full sql commands to create the necessary indexes and columns
-- This script has been run manually on the dev, pre, and prod database to improve performance of soundex searches
-- This script is very slow when run as a flyway migration due to the table rewrite and indexing
-- Therefore, it is only included here for testing purposes, tracking, and documentation


-- Add GIN trigram index for contactid search
CREATE INDEX idx_contact_contactid_gin ON contact USING gin ((contact_id::text) gin_trgm_ops);

-- Add soundex generated columns for better performance on soundex searches
ALTER TABLE contact ADD COLUMN last_name_soundex text GENERATED ALWAYS AS (soundex(last_name)) STORED;
ALTER TABLE contact ADD COLUMN first_name_soundex text GENERATED ALWAYS AS (soundex(first_name)) STORED;
ALTER TABLE contact ADD COLUMN middle_names_soundex text GENERATED ALWAYS AS (soundex(middle_names)) STORED;

-- Create indexes on soundex columns for faster lookups
CREATE INDEX idx_contact_last_name_soundex ON contact(last_name_soundex);
CREATE INDEX idx_contact_first_name_soundex ON contact(first_name_soundex);
CREATE INDEX idx_contact_middle_names_soundex ON contact(middle_names_soundex);
