--
-- Update description of phone number type options
--
UPDATE reference_codes SET description = 'Home' WHERE group_code = 'PHONE_TYPE' and code = 'HOME';
UPDATE reference_codes SET description = 'Business' WHERE group_code = 'PHONE_TYPE' and code = 'BUS';
UPDATE reference_codes SET description = 'Fax' WHERE group_code = 'PHONE_TYPE' and code = 'FAX';
UPDATE reference_codes SET description = 'Alternate business' WHERE group_code = 'PHONE_TYPE' and code = 'ALTB';
UPDATE reference_codes SET description = 'Alternate home' WHERE group_code = 'PHONE_TYPE' and code = 'ALTH';
UPDATE reference_codes SET description = 'Mobile' WHERE group_code = 'PHONE_TYPE' and code = 'MOB';
UPDATE reference_codes SET description = 'Agency visit line' WHERE group_code = 'PHONE_TYPE' and code = 'VISIT';
-- End