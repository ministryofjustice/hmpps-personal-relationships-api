
-- Migration: V2025.11.12.45__improve_soundex_search.sql
-- Important note about this file:
--   This is a stripped, Flyway migration kept for tracking/failsafe purposes.
-- 'GENERATED ALWAYS AS (soundex(last_name)) STORED' columns have been replaced with regular text columns.
--  The full
--   'ALTER TABLE contact
--      ADD COLUMN IF NOT EXISTS last_name_soundex text GENERATED ALWAYS AS (soundex(last_name)) STORED'
-- was applied manually to dev, preprod, and production environments.
-- Above requires a full table rewrite and acquiring an exclusive lock on large tables.
-- Therefore, the flyway migration is timeout causing container to crash,
-- Figured that it is not suitable for automated deployment via Flyway in environments with a larger quantity of data.
ALTER TABLE contact
    ADD COLUMN IF NOT EXISTS last_name_soundex text,
    ADD COLUMN IF NOT EXISTS first_name_soundex text,
    ADD COLUMN IF NOT EXISTS middle_names_soundex text;

-- Add indexes on soundex columns for faster searches
CREATE INDEX IF NOT EXISTS idx_contact_last_name_soundex ON contact(last_name_soundex);
CREATE INDEX IF NOT EXISTS idx_contact_first_name_soundex ON contact(first_name_soundex);
CREATE INDEX IF NOT EXISTS idx_contact_middle_names_soundex ON contact(middle_names_soundex);

-- Add GIN trigram index for contactid search
CREATE INDEX IF NOT EXISTS idx_contact_contactid_gin ON contact USING gin ((contact_id::text) gin_trgm_ops);
