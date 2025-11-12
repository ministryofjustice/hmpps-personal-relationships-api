
-- Add soundex generated columns for better performance on soundex searches
-- this is only a stripped version of the full flyway script
-- the original database change has been deployed manually
-- This is because the change can't be installed as a flyway script due to slowness in the table rewrite and indexingt

ALTER TABLE contact
    ADD COLUMN IF NOT EXISTS last_name_soundex text,
    ADD COLUMN IF NOT EXISTS first_name_soundex text,
    ADD COLUMN IF NOT EXISTS middle_names_soundex text;

CREATE INDEX IF NOT EXISTS idx_contact_last_name_soundex ON contact(last_name_soundex);
CREATE INDEX IF NOT EXISTS idx_contact_first_name_soundex ON contact(first_name_soundex);
CREATE INDEX IF NOT EXISTS idx_contact_middle_names_soundex ON contact(middle_names_soundex);

