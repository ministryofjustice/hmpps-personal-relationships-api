-- ==========================================================
-- Example data
-- Not loaded into any real environments - DEV, PREPROD or PROD
-- Intended for integration tests and local-running only.
-- ===========================================================
update prisoner_contact set approved_by = 'A_USER' where prisoner_contact_id = 1 and contact_id =1;