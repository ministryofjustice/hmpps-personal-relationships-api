-- Remove unused indexes from contact
DROP index IF EXISTS idx_soundex_lastname;
DROP index IF EXISTS idx_soundex_firstname;
DROP index IF EXISTS idx_soundex_middlenames;

-- Might also want to remove this one (for partial contactId search) sooner rather than later
-- DROP INDEX IF EXISTS idx_contact_id_as_text;
