--
-- Update description of domestic status options
--
UPDATE reference_codes SET description = 'Single (not married or in a civil partnership)' WHERE group_code = 'DOMESTIC_STS' and code = 'S';
UPDATE reference_codes SET description = 'Married or in a civil partnership' WHERE group_code = 'DOMESTIC_STS' and code = 'M';
UPDATE reference_codes SET description = 'Divorced or dissolved marriage' WHERE group_code = 'DOMESTIC_STS' and code = 'D';
UPDATE reference_codes SET description = 'Separated (no longer living with legal partner)' WHERE group_code = 'DOMESTIC_STS' and code = 'P';
UPDATE reference_codes SET description = 'Not known' WHERE group_code = 'DOMESTIC_STS' and code = 'N';
-- End