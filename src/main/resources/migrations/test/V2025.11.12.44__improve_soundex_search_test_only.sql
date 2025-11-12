
-- SOUNDEX SEARCH PERFORMANCE OPTIMIZATION
--
-- Purpose: Add generated columns for soundex values to improve phonetic search performance
--
-- Status: MANUALLY DEPLOYED to dev, pre, and prod environments
--
-- Note: This file serves ONLY for testing, tracking, and documentation purposes.
--       It is only executed by Flyway during the test data setup
--       These database changes have already been applied manually in all environments.
-- 'GENERATED ALWAYS AS (soundex(last_name)) STORED' is used to create a computed column
-- that automatically stores the Soundex value of the name fields.
-- This improves search performance by avoiding on-the-fly computation during queries.

ALTER TABLE contact
    ADD COLUMN IF NOT EXISTS last_name_soundex text GENERATED ALWAYS AS (soundex(last_name)) STORED,
    ADD COLUMN IF NOT EXISTS first_name_soundex text GENERATED ALWAYS AS (soundex(first_name)) STORED,
    ADD COLUMN IF NOT EXISTS middle_names_soundex text GENERATED ALWAYS AS (soundex(middle_names)) STORED;