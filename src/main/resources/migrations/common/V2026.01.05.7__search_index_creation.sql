-- Indexes are create with IF NOT EXISTS so that we could pre-create these with the CONCURRENTLY option
-- ahead of this migration being applied, without locking tables.

-- Index the soundex columns on contact
create index IF NOT EXISTS idx_contact_last_name_soundex on contact (last_name_soundex);
create index IF NOT EXISTS idx_contact_first_name_soundex on contact (first_name_soundex);
create index IF NOT EXISTS idx_contact_middle_names_soundex on contact (middle_names_soundex);

-- Index the soundex name columns on contact_audit
create index IF NOT EXISTS idx_contact_audit_last_name_soundex on contact_audit (last_name_soundex);
create index IF NOT EXISTS idx_contact_audit_first_name_soundex on contact_audit (first_name_soundex);
create index IF NOT EXISTS idx_contact_audit_middle_names_soundex on contact_audit (middle_names_soundex);

-- Add standard indexes on contact audit prisoner name and DOB columns
create index IF NOT EXISTS idx_contact_audit_last_name on contact_audit (last_name);
create index IF NOT EXISTS idx_contact_audit_first_name on contact_audit (first_name);
create index IF NOT EXISTS idx_contact_audit_middle_names on contact_audit (middle_names);
create index IF NOT EXISTS idx_contact_audit_date_of_birth on contact_audit (date_of_birth);

-- Add GIN indexes on contact_audit name columns
create index IF NOT EXISTS idx_contact_audit_last_name_gin on contact_audit using gin (last_name gin_trgm_ops);
create index IF NOT EXISTS idx_contact_audit_first_name_gin on contact_audit using gin (first_name gin_trgm_ops);
create index IF NOT EXISTS idx_contact_audit_middle_names_gin on contact_audit using gin (middle_names gin_trgm_ops);

-- End