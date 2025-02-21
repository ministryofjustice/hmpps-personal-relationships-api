--
-- Contact identity value and issuing authority were longer than in NOMIS or than validation allowed. Reducing the size
-- to ensure we don't accidentally start accepting longer values that cannot be synced to NOMIS.
-- Also reducing length of reference_codes.code to 12 as all columns backed by reference data are also set to 12
-- as per NOMIS. This prevents us adding a code that is not usable in practice.
--

ALTER TABLE contact_identity ALTER COLUMN identity_value TYPE VARCHAR(20);
ALTER TABLE contact_identity ALTER COLUMN issuing_authority TYPE VARCHAR(40);

ALTER TABLE reference_codes ALTER COLUMN code TYPE VARCHAR(12);

-- End