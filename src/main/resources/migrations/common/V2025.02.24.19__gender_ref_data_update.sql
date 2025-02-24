--
-- Re-order and update description of gender options
--

UPDATE reference_codes SET display_order = 3 WHERE group_code = 'GENDER' and code = 'NS';
UPDATE reference_codes SET description = 'Not known', display_order = 4 WHERE group_code = 'GENDER' and code = 'NK';

-- End