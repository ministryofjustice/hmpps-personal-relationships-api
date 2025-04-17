--
-- Update order of title options
--

UPDATE reference_codes SET display_order = 1 WHERE group_code = 'TITLE' AND code = 'MR';
UPDATE reference_codes SET display_order = 2 WHERE group_code = 'TITLE' AND code = 'MRS';
UPDATE reference_codes SET display_order = 3 WHERE group_code = 'TITLE' AND code = 'MISS';
UPDATE reference_codes SET display_order = 4 WHERE group_code = 'TITLE' AND code = 'MS';
UPDATE reference_codes SET display_order = 5 WHERE group_code = 'TITLE' AND code = 'REV';
UPDATE reference_codes SET display_order = 6 WHERE group_code = 'TITLE' AND code = 'FR';
UPDATE reference_codes SET display_order = 7 WHERE group_code = 'TITLE' AND code = 'IMAM';
UPDATE reference_codes SET display_order = 8 WHERE group_code = 'TITLE' AND code = 'RABBI';
UPDATE reference_codes SET display_order = 9 WHERE group_code = 'TITLE' AND code = 'BR';
UPDATE reference_codes SET display_order = 10 WHERE group_code = 'TITLE' AND code = 'SR';
UPDATE reference_codes SET display_order = 11 WHERE group_code = 'TITLE' AND code = 'DAME';
UPDATE reference_codes SET display_order = 12 WHERE group_code = 'TITLE' AND code = 'DR';
UPDATE reference_codes SET display_order = 13 WHERE group_code = 'TITLE' AND code = 'LADY';
UPDATE reference_codes SET display_order = 14 WHERE group_code = 'TITLE' AND code = 'LORD';
UPDATE reference_codes SET display_order = 15 WHERE group_code = 'TITLE' AND code = 'SIR';

-- End