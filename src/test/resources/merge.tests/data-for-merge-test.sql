-- ==================================================================
-- Test data for prisoner contact merge integration test
-- Requires the fixed IDs to tie the data together
-- (might be better to create these as part of the test using the API)
-- ==================================================================

insert into contact(contact_id, title, last_name, first_name, date_of_birth, gender, domestic_status, language_code, created_by, interpreter_required, staff_flag)
values (30001,  'MR', 'Ma', 'John', '2000-11-21', 'M', 'M', 'ENG', 'TIM', false, false),
       (30002,  'MR', 'Mb', 'Jack', '2000-11-22', 'M', 'D', 'ENG', 'TIM', false, false),
       (30003,  'MR', 'Mc', 'Joly', '2000-11-23', 'M', 'S', 'ENG', 'TIM', false, false),
       (30004,  'MR', 'Md', 'Jam',  '2000-11-24', 'M', 'S', 'ENG', 'TIM', false, false),
       (30005,  'MR', 'Me', 'Jim',  '2000-11-25', 'M', 'S', 'ENG', 'TIM', false, false);

insert into prisoner_contact (prisoner_contact_id, contact_id, prisoner_number, relationship_type, active, relationship_to_prisoner, created_at_prison, created_by, created_time)
values (40001, 30001, 'A3333AA', 'S', true, 'BRO', 'MDI', 'TIM', current_timestamp),
       (40002, 30002, 'A3333AA', 'O', true, 'POL', 'MDI', 'TIM', current_timestamp),
       (40003, 30003, 'A4444AA', 'S', true, 'MOT', 'MDI', 'TIM', current_timestamp),
       (40004, 30004, 'A4444AA', 'S', true, 'SIS', 'MDI', 'TIM', current_timestamp),
       (40005, 30005, 'A4444AA', 'O', true, 'POL', 'MDI', 'TIM', current_timestamp);

insert into prisoner_contact_restriction (prisoner_contact_id, restriction_type, start_date, expiry_date, comments, created_by, created_time)
values (40001, 'BAN', '2025-01-01', '2025-05-02', 'Restriction A', 'TIM', current_timestamp),
       (40002, 'BAN', '2025-01-02', '2025-05-01', 'Restriction B', 'TIM', current_timestamp);
