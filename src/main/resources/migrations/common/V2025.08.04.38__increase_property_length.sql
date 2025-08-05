DROP VIEW IF EXISTS v_contact_addresses;
DROP VIEW IF EXISTS v_contacts_with_primary_address;
DROP VIEW IF EXISTS v_prisoner_contacts;

ALTER TABLE contact_address ALTER COLUMN property TYPE VARCHAR(130);