
-- Standard indexes on audit columns
create index idx_contact_audit_last_name on contact_audit (last_name);
create index idx_contact_audit_first_name on contact_audit (first_name);
create index idx_contact_audit_middle_names on contact_audit (middle_names);

-- Soundex on audit columns
create index idx_soundex_audit_last_name on contact_audit (soundex(last_name));
create index idx_soundex_audit_first_name on contact_audit (soundex(first_name));
create index idx_soundex_audit_middle_names on contact_audit (soundex(middle_names));

-- GIN indexes on contact_audit names
create index idx_contact_audit_last_name_gin on contact_audit using gin (last_name gin_trgm_ops);
create index idx_contact_audit_first_name_gin on contact_audit using gin (first_name gin_trgm_ops);
create index idx_contact_audit_middle_names_gin on contact_audit using gin (middle_names gin_trgm_ops);