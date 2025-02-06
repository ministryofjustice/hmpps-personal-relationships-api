--
-- The two were incorrectly set as integers despite being modelled as longs in Kotlin allowing integer overflow.
-- This breaks the dev environment now that there are millions of contacts so needs to be rolled into main scripts
-- when we next wipe the db.
--
DROP VIEW IF EXISTS v_contact_addresses;
DROP VIEW IF EXISTS v_contact_identities;
DROP VIEW IF EXISTS v_contact_phone_numbers;
DROP VIEW IF EXISTS v_contact_restriction_details;
DROP VIEW IF EXISTS v_contacts_with_primary_address;
DROP VIEW IF EXISTS v_organisation_addresses;
DROP VIEW IF EXISTS v_organisation_phone_numbers;
DROP VIEW IF EXISTS v_organisation_summary;
DROP VIEW IF EXISTS v_organisation_types;
DROP VIEW IF EXISTS v_prisoner_contact_restriction_details;
DROP VIEW IF EXISTS v_prisoner_contacts;

ALTER TABLE contact ALTER COLUMN contact_id TYPE BIGINT;
ALTER TABLE organisation ALTER COLUMN organisation_id TYPE BIGINT;

-- End