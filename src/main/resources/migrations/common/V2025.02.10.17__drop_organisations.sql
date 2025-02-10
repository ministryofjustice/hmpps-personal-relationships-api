--
-- The two were incorrectly set as integers despite being modelled as longs in Kotlin allowing integer overflow.
-- This breaks the dev environment now that there are millions of contacts so needs to be rolled into main scripts
-- when we next wipe the db.
--
DROP VIEW IF EXISTS v_organisation_addresses;
DROP VIEW IF EXISTS v_organisation_phone_numbers;
DROP VIEW IF EXISTS v_organisation_summary;
DROP VIEW IF EXISTS v_organisation_types;

DROP TABLE organisation_address_phone;
DROP TABLE organisation_address;
DROP TABLE organisation_web_address;
DROP TABLE organisation_email;
DROP TABLE organisation_phone;
DROP TABLE organisation_type;
DROP TABLE organisation;

DELETE FROM reference_codes WHERE group_code = 'ORGANISATION_TYPE';
DELETE FROM reference_codes WHERE group_code = 'ORG_ADDRESS_SPECIAL_NEEDS';

-- End