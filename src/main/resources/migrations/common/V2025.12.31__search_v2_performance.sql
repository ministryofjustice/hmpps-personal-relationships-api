
-- Remove unused indexes from contact - more effective indexes are created below
DROP index idx_soundex_lastname;
DROP index idx_soundex_firstname;
DROP index idx_soundex_middlenames;

-- Add soundex GENERATED columns to contact (2 mins each on DEV)
ALTER TABLE contact ADD COLUMN last_name_soundex CHAR(4) GENERATED ALWAYS AS (soundex(last_name)) STORED;
ALTER TABLE contact ADD COLUMN first_name_soundex CHAR(4) GENERATED ALWAYS AS (soundex(first_name)) STORED;
ALTER TABLE contact ADD COLUMN middle_names_soundex CHAR(4) GENERATED ALWAYS AS (soundex(middle_names)) STORED;

-- Index the soundex columns on contact
create index idx_contact_last_name_soundex on contact (last_name_soundex);
create index idx_contact_first_name_soundex on contact (first_name_soundex);
create index idx_contact_middle_names_soundex on contact (middle_names_soundex);

-- Add standard indexes on contact audit prisoner name and DOB columns
create index idx_contact_audit_last_name on contact_audit (last_name);
create index idx_contact_audit_first_name on contact_audit (first_name);
create index idx_contact_audit_middle_names on contact_audit (middle_names);
create index idx_contact_audit_date_of_birth on contact_audit (date_of_birth);

-- Add GIN indexes on contact_audit name columns
create index idx_contact_audit_last_name_gin on contact_audit using gin (last_name gin_trgm_ops);
create index idx_contact_audit_first_name_gin on contact_audit using gin (first_name gin_trgm_ops);
create index idx_contact_audit_middle_names_gin on contact_audit using gin (middle_names gin_trgm_ops);

-- Add the soundex GENERATED columns on contact_audit name columns (2 mins each on DEV)
ALTER TABLE contact_audit ADD COLUMN last_name_soundex CHAR(4) GENERATED ALWAYS AS (soundex(last_name)) STORED;
ALTER TABLE contact_audit ADD COLUMN first_name_soundex CHAR(4) GENERATED ALWAYS AS (soundex(first_name)) STORED;
ALTER TABLE contact_audit ADD COLUMN middle_names_soundex CHAR(4) GENERATED ALWAYS AS (soundex(middle_names)) STORED;

-- Index the soundex name columns on contact_audit
create index idx_contact_audit_last_name_soundex on contact_audit (last_name_soundex);
create index idx_contact_audit_first_name_soundex on contact_audit (first_name_soundex);
create index idx_contact_audit_middle_names_soundex on contact_audit (middle_names_soundex);

-- End